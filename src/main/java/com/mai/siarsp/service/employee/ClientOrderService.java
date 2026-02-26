package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
import com.mai.siarsp.models.Client;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.repo.ClientOrderRepository;
import com.mai.siarsp.repo.ClientRepository;
import com.mai.siarsp.repo.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис управления заказами клиентов
 *
 * Бизнес-процесс:
 * 1. Директор создаёт заказ → статус NEW
 * 2. Директор подтверждает → CONFIRMED, уведомление заведующему складом
 * 3. Заведующий резервирует товар → RESERVED, product.reservedQuantity↑
 * 4. Заведующий собирает заказ → IN_PROGRESS
 * 5. Заведующий завершает сборку → READY, уведомление директору
 * 6. Отмена (из NEW/CONFIRMED/RESERVED) → CANCELLED, откат резерва если нужно
 */
@Service
@Slf4j
public class ClientOrderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final Random RANDOM = new Random();

    private final ClientOrderRepository clientOrderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;

    public ClientOrderService(ClientOrderRepository clientOrderRepository,
                              ClientRepository clientRepository,
                              ProductRepository productRepository,
                              NotificationService notificationService) {
        this.clientOrderRepository = clientOrderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
    }

    // ========== ЗАПРОСЫ ==========

    /**
     * Запрос на добавление позиции в заказ
     */
    public record OrderItemRequest(Long productId, int quantity, BigDecimal price) {}

    /**
     * Получает все заказы (новейшие первыми)
     */
    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getAllOrders() {
        List<ClientOrder> orders = clientOrderRepository.findAllByOrderByOrderDateDesc();
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    /**
     * Получает заказы по статусу
     */
    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByStatus(ClientOrderStatus status) {
        List<ClientOrder> orders = clientOrderRepository.findByStatusOrderByOrderDateDesc(status);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    /**
     * Получает заказы по нескольким статусам
     */
    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByStatuses(List<ClientOrderStatus> statuses) {
        List<ClientOrder> orders = clientOrderRepository.findByStatusInOrderByOrderDateDesc(statuses);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    /**
     * Получает заказы клиента
     */
    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByClient(Long clientId) {
        List<ClientOrder> orders = clientOrderRepository.findByClientIdOrderByOrderDateDesc(clientId);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    /**
     * Получает заказ по идентификатору
     */
    @Transactional(readOnly = true)
    public Optional<ClientOrder> getOrderById(Long id) {
        return clientOrderRepository.findByIdWithDetails(id);
    }

    // ========== СОЗДАНИЕ И РЕДАКТИРОВАНИЕ ==========

    /**
     * Создаёт новый заказ клиента
     *
     * @param clientId    идентификатор клиента
     * @param deliveryDate планируемая дата доставки
     * @param comment     комментарий
     * @param items       позиции заказа (товар, количество, цена)
     * @param responsible ответственный сотрудник (директор)
     * @return true при успешном создании
     */
    @Transactional
    public boolean createOrder(Long clientId, LocalDate deliveryDate, String comment,
                               List<OrderItemRequest> items, Employee responsible) {
        try {
            if (items == null || items.isEmpty()) {
                log.error("Список позиций заказа пуст");
                return false;
            }

            Optional<Client> optClient = clientRepository.findById(clientId);
            if (optClient.isEmpty()) {
                log.error("Клиент с id={} не найден", clientId);
                return false;
            }

            String orderNumber = generateOrderNumber();
            ClientOrder order = new ClientOrder(orderNumber, optClient.get(), responsible, deliveryDate);
            order.setComment(comment);

            for (OrderItemRequest item : items) {
                Optional<Product> optProduct = productRepository.findById(item.productId());
                if (optProduct.isEmpty()) {
                    log.error("Товар с id={} не найден", item.productId());
                    return false;
                }

                if (item.quantity() <= 0) {
                    log.error("Некорректное количество: {}", item.quantity());
                    return false;
                }

                if (item.price() == null || item.price().compareTo(BigDecimal.ZERO) <= 0) {
                    log.error("Некорректная цена: {}", item.price());
                    return false;
                }

                OrderedProduct orderedProduct = new OrderedProduct(optProduct.get(), item.quantity(), item.price());
                order.addOrderedProduct(orderedProduct);
            }

            order.calculateTotalAmount();
            clientOrderRepository.save(order);

            log.info("Создан заказ №{}: клиент '{}', позиций {}, сумма {}",
                    orderNumber, optClient.get().getOrganizationName(),
                    items.size(), order.getTotalAmount());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при создании заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Редактирует заказ (только в статусе NEW)
     */
    @Transactional
    public boolean updateOrder(Long orderId, LocalDate deliveryDate, String comment,
                               List<OrderItemRequest> items) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.NEW) {
                log.error("Редактирование заказа №{} невозможно — статус {}", order.getOrderNumber(), order.getStatus());
                return false;
            }

            if (items == null || items.isEmpty()) {
                log.error("Список позиций заказа пуст");
                return false;
            }

            order.setDeliveryDate(deliveryDate);
            order.setComment(comment);

            // Smart merge: обновляем существующие, добавляем новые, удаляем лишние
            // (вместо clear() + re-add, чтобы избежать Duplicate Entry при flush)
            Map<Long, OrderItemRequest> incoming = new LinkedHashMap<>();
            for (OrderItemRequest item : items) {
                if (item.quantity() <= 0 || item.price() == null || item.price().compareTo(BigDecimal.ZERO) <= 0) {
                    log.error("Некорректные данные позиции: qty={}, price={}", item.quantity(), item.price());
                    return false;
                }
                incoming.put(item.productId(), item);
            }

            Map<Long, OrderedProduct> existing = new HashMap<>();
            for (OrderedProduct op : order.getOrderedProducts()) {
                existing.put(op.getProduct().getId(), op);
            }

            // UPDATE существующих + ADD новых
            for (Map.Entry<Long, OrderItemRequest> entry : incoming.entrySet()) {
                Long pid = entry.getKey();
                OrderItemRequest item = entry.getValue();

                OrderedProduct op = existing.get(pid);
                if (op != null) {
                    op.setQuantity(item.quantity());
                    op.setPrice(item.price());
                    op.recalculateTotalPrice();
                } else {
                    Optional<Product> optProduct = productRepository.findById(pid);
                    if (optProduct.isEmpty()) {
                        log.error("Товар с id={} не найден", pid);
                        return false;
                    }
                    OrderedProduct newOp = new OrderedProduct(optProduct.get(), item.quantity(), item.price());
                    order.addOrderedProduct(newOp);
                }
            }

            // REMOVE позиций, которых больше нет во входе
            order.getOrderedProducts().removeIf(op -> !incoming.containsKey(op.getProduct().getId()));

            order.calculateTotalAmount();
            clientOrderRepository.save(order);

            log.info("Заказ №{} обновлён: позиций {}, сумма {}",
                    order.getOrderNumber(), items.size(), order.getTotalAmount());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при обновлении заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== СМЕНА СТАТУСОВ ==========

    /**
     * Подтверждает заказ (NEW → CONFIRMED)
     * Уведомление заведующему складом
     */
    @Transactional
    public boolean confirmOrder(Long orderId) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.NEW) {
                log.error("Заказ №{} не в статусе NEW (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            if (order.getOrderedProducts().isEmpty()) {
                log.error("Заказ №{} не содержит позиций", order.getOrderNumber());
                return false;
            }

            order.setStatus(ClientOrderStatus.CONFIRMED);
            clientOrderRepository.save(order);

            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " подтверждён. Клиент: "
                            + order.getClient().getOrganizationName()
                            + ". Ожидает резервирования товара.");

            log.info("Заказ №{} подтверждён", order.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при подтверждении заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Резервирует товар по заказу (CONFIRMED → RESERVED)
     * Атомарная операция: сначала проверяет ВСЕ позиции, потом резервирует
     */
    @Transactional
    public boolean reserveProducts(Long orderId) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.CONFIRMED) {
                log.error("Заказ №{} не в статусе CONFIRMED (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            // Проверяем доступность ВСЕХ товаров перед резервированием
            for (OrderedProduct op : order.getOrderedProducts()) {
                Product product = op.getProduct();
                if (product.getAvailableQuantity() < op.getQuantity()) {
                    log.error("Недостаточно товара '{}': требуется {}, доступно {}",
                            product.getName(), op.getQuantity(), product.getAvailableQuantity());
                    return false;
                }
            }

            // Все проверки пройдены — резервируем
            for (OrderedProduct op : order.getOrderedProducts()) {
                Product product = op.getProduct();
                product.setReservedQuantity(product.getReservedQuantity() + op.getQuantity());
                productRepository.save(product);
            }

            order.setStatus(ClientOrderStatus.RESERVED);
            clientOrderRepository.save(order);

            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Товар по заказу №" + order.getOrderNumber() + " зарезервирован. Клиент: "
                            + order.getClient().getOrganizationName());

            log.info("Заказ №{}: товар зарезервирован ({} позиций)",
                    order.getOrderNumber(), order.getOrderedProducts().size());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при резервировании товара: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Начинает сборку заказа (RESERVED → IN_PROGRESS)
     */
    @Transactional
    public boolean startAssembly(Long orderId) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.RESERVED) {
                log.error("Заказ №{} не в статусе RESERVED (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            order.setStatus(ClientOrderStatus.IN_PROGRESS);
            clientOrderRepository.save(order);

            log.info("Заказ №{}: начата сборка", order.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при начале сборки заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Завершает сборку заказа (IN_PROGRESS → READY)
     * Уведомление директору
     */
    @Transactional
    public boolean completeAssembly(Long orderId) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();

            if (order.getStatus() != ClientOrderStatus.IN_PROGRESS) {
                log.error("Заказ №{} не в статусе IN_PROGRESS (текущий: {})", order.getOrderNumber(), order.getStatus());
                return false;
            }

            order.setStatus(ClientOrderStatus.READY);
            clientOrderRepository.save(order);

            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " готов к отгрузке. Клиент: "
                            + order.getClient().getOrganizationName());

            log.info("Заказ №{}: сборка завершена, готов к отгрузке", order.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при завершении сборки заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    /**
     * Отменяет заказ (из NEW, CONFIRMED или RESERVED)
     * Если заказ был RESERVED — откатывает резервирование
     */
    @Transactional
    public boolean cancelOrder(Long orderId) {
        try {
            Optional<ClientOrder> optOrder = clientOrderRepository.findById(orderId);
            if (optOrder.isEmpty()) {
                log.error("Заказ с id={} не найден", orderId);
                return false;
            }

            ClientOrder order = optOrder.get();
            ClientOrderStatus currentStatus = order.getStatus();

            if (currentStatus != ClientOrderStatus.NEW
                    && currentStatus != ClientOrderStatus.CONFIRMED
                    && currentStatus != ClientOrderStatus.RESERVED) {
                log.error("Отмена заказа №{} невозможна — статус {}", order.getOrderNumber(), currentStatus);
                return false;
            }

            // Откат резервирования
            if (currentStatus == ClientOrderStatus.RESERVED) {
                for (OrderedProduct op : order.getOrderedProducts()) {
                    Product product = op.getProduct();
                    product.setReservedQuantity(
                            Math.max(0, product.getReservedQuantity() - op.getQuantity()));
                    productRepository.save(product);
                }
                log.info("Заказ №{}: резервирование отменено", order.getOrderNumber());
            }

            order.setStatus(ClientOrderStatus.CANCELLED);
            clientOrderRepository.save(order);

            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " отменён. Клиент: "
                            + order.getClient().getOrganizationName());

            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER",
                    "Заказ №" + order.getOrderNumber() + " отменён.");

            log.info("Заказ №{} отменён (предыдущий статус: {})", order.getOrderNumber(), currentStatus);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при отмене заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== АНАЛИТИКА ==========

    /**
     * Возвращает дефицит товаров по активным заказам (NEW, CONFIRMED)
     * Дефицит = суммарная потребность по заказам - доступное количество на складе
     *
     * Используется на странице создания заявки на поставку
     * для информирования заведующего складом о нехватке товаров
     */
    @Transactional(readOnly = true)
    public List<Map<String, Object>> getProductDeficit() {
        List<ClientOrderStatus> activeStatuses = List.of(
                ClientOrderStatus.NEW, ClientOrderStatus.CONFIRMED);
        List<ClientOrder> activeOrders = clientOrderRepository.findByStatusInOrderByOrderDateDesc(activeStatuses);

        // Суммируем потребность по товарам
        Map<Long, Integer> demandByProduct = new HashMap<>();
        Map<Long, Product> productsMap = new HashMap<>();

        for (ClientOrder order : activeOrders) {
            for (OrderedProduct op : order.getOrderedProducts()) {
                Long pid = op.getProduct().getId();
                demandByProduct.merge(pid, op.getQuantity(), Integer::sum);
                productsMap.putIfAbsent(pid, op.getProduct());
            }
        }

        // Собираем дефицитные позиции
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : demandByProduct.entrySet()) {
            Long pid = entry.getKey();
            int orderedTotal = entry.getValue();
            Product product = productsMap.get(pid);
            int available = product.getAvailableQuantity();

            if (orderedTotal > available) {
                int deficit = orderedTotal - available;
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("productId", pid);
                item.put("productName", product.getName());
                item.put("article", product.getArticle());
                item.put("orderedTotal", orderedTotal);
                item.put("stockQuantity", product.getStockQuantity());
                item.put("availableQuantity", available);
                item.put("deficit", deficit);
                result.add(item);
            }
        }

        result.sort(Comparator.comparing(m -> (String) m.get("productName")));
        return result;
    }

    // ========== ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ==========

    /**
     * Генерирует уникальный номер заказа в формате ЗК-YYYYMMDD-NNNN
     */
    private String generateOrderNumber() {
        String datePart = LocalDate.now().format(DATE_FMT);
        String orderNumber;
        do {
            int number = 1000 + RANDOM.nextInt(9000);
            orderNumber = "ЗК-" + datePart + "-" + number;
        } while (clientOrderRepository.existsByOrderNumber(orderNumber));
        return orderNumber;
    }
}
