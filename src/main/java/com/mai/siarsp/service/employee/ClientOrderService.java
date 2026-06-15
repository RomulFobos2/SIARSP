package com.mai.siarsp.service.employee;

import com.mai.siarsp.dto.ClientOrderDTO;
import com.mai.siarsp.enumeration.ClientOrderStatus;
import com.mai.siarsp.mapper.ClientOrderMapper;
import com.mai.siarsp.models.Client;
import com.mai.siarsp.models.ClientOrder;
import com.mai.siarsp.models.Employee;
import com.mai.siarsp.models.OrderedProduct;
import com.mai.siarsp.models.OrderedProductPick;
import com.mai.siarsp.models.Product;
import com.mai.siarsp.models.StorageZone;
import com.mai.siarsp.models.Supply;
import com.mai.siarsp.models.ZoneProduct;
import com.mai.siarsp.repo.ClientOrderRepository;
import com.mai.siarsp.repo.ClientRepository;
import com.mai.siarsp.repo.OrderedProductPickRepository;
import com.mai.siarsp.repo.OrderedProductRepository;
import com.mai.siarsp.repo.ProductRepository;
import com.mai.siarsp.repo.StorageZoneRepository;
import com.mai.siarsp.repo.SupplyRepository;
import com.mai.siarsp.repo.ZoneProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import com.mai.siarsp.service.general.ContractService;
import com.mai.siarsp.service.general.ProductExpirationService;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Сервис жизненного цикла клиентского заказа: создание, смена статусов, контроль комплектности и подготовка к отгрузке.
 */

@Service
@Slf4j
public class ClientOrderService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter DISPLAY_DATE_FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final Random RANDOM = new Random();

    private final ClientOrderRepository clientOrderRepository;
    private final ClientRepository clientRepository;
    private final ProductRepository productRepository;
    private final NotificationService notificationService;
    private final ProductExpirationService productExpirationService;
    private final OrderedProductRepository orderedProductRepository;
    private final OrderedProductPickRepository orderedProductPickRepository;
    private final SupplyRepository supplyRepository;
    private final StorageZoneRepository storageZoneRepository;
    private final ZoneProductRepository zoneProductRepository;

    public ClientOrderService(ClientOrderRepository clientOrderRepository,
                              ClientRepository clientRepository,
                              ProductRepository productRepository,
                              NotificationService notificationService,
                              ProductExpirationService productExpirationService,
                              OrderedProductRepository orderedProductRepository,
                              OrderedProductPickRepository orderedProductPickRepository,
                              SupplyRepository supplyRepository,
                              StorageZoneRepository storageZoneRepository,
                              ZoneProductRepository zoneProductRepository) {
        this.clientOrderRepository = clientOrderRepository;
        this.clientRepository = clientRepository;
        this.productRepository = productRepository;
        this.notificationService = notificationService;
        this.productExpirationService = productExpirationService;
        this.orderedProductRepository = orderedProductRepository;
        this.orderedProductPickRepository = orderedProductPickRepository;
        this.supplyRepository = supplyRepository;
        this.storageZoneRepository = storageZoneRepository;
        this.zoneProductRepository = zoneProductRepository;
    }

    // ========== ЗАПРОСЫ ==========

    /**
     * Запрос на добавление позиции в заказ
     */
    public record OrderItemRequest(Long productId, int quantity, BigDecimal price,
                                    BigDecimal originalPrice, Integer discountPercent,
                                    Integer markupPercent) {}

    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getAllOrders() {
        List<ClientOrder> orders = clientOrderRepository.findAllByOrderByOrderDateDesc();
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByStatus(ClientOrderStatus status) {
        List<ClientOrder> orders = clientOrderRepository.findByStatusOrderByOrderDateDesc(status);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByStatuses(List<ClientOrderStatus> statuses) {
        List<ClientOrder> orders = clientOrderRepository.findByStatusInOrderByOrderDateDesc(statuses);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    @Transactional(readOnly = true)
    public List<ClientOrderDTO> getOrdersByClient(Long clientId) {
        List<ClientOrder> orders = clientOrderRepository.findByClientIdOrderByOrderDateDesc(clientId);
        return ClientOrderMapper.INSTANCE.toDTOList(orders);
    }

    @Transactional(readOnly = true)
    public Optional<ClientOrder> getOrderById(Long id) {
        return clientOrderRepository.findByIdWithDetails(id);
    }

    // ========== СОЗДАНИЕ И РЕДАКТИРОВАНИЕ ==========

    @Transactional
    public boolean createOrder(Long clientId, LocalDate deliveryDate, String comment,
                               List<OrderItemRequest> items, Employee responsible,
                               MultipartFile contractFile) {
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

            // Загрузка файла контракта
            String contractFileName = ContractService.uploadContract(contractFile);
            order.setContractFile(contractFileName);

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

                OrderedProduct orderedProduct = new OrderedProduct(optProduct.get(), item.quantity(), item.price(),
                        item.originalPrice(), item.discountPercent(), item.markupPercent());
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

    @Transactional
    public boolean updateOrder(Long orderId, LocalDate deliveryDate, String comment,
                               List<OrderItemRequest> items, MultipartFile contractFile) {
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

            // Замена файла контракта (если загружен новый)
            if (contractFile != null && !contractFile.isEmpty()) {
                if (order.getContractFile() != null && !order.getContractFile().isBlank()) {
                    ContractService.deleteContract(order.getContractFile());
                }
                String newContractFileName = ContractService.uploadContract(contractFile);
                order.setContractFile(newContractFileName);
            }

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
                    op.setOriginalPrice(item.originalPrice());
                    op.setDiscountPercent(item.discountPercent());
                    op.setMarkupPercent(item.markupPercent());
                    op.recalculateTotalPrice();
                } else {
                    Optional<Product> optProduct = productRepository.findById(pid);
                    if (optProduct.isEmpty()) {
                        log.error("Товар с id={} не найден", pid);
                        return false;
                    }
                    OrderedProduct newOp = new OrderedProduct(optProduct.get(), item.quantity(), item.price(),
                            item.originalPrice(), item.discountPercent(), item.markupPercent());
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

            List<String> expiringProducts = collectProductsExpiringBefore(order, order.getDeliveryDate());
            if (!expiringProducts.isEmpty()) {
                log.error("Заказ №{} нельзя подтвердить: срок годности истечёт до даты доставки у товаров: {}",
                        order.getOrderNumber(), expiringProducts);
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
     * Возвращает названия товаров заказа, срок годности которых истекает раньше плановой даты доставки.
     * Используется контроллером для UX-сообщения перед подтверждением и внутри confirmOrder как защита.
     */
    @Transactional(readOnly = true)
    public List<String> findProductsExpiringBeforeDelivery(Long orderId) {
        return clientOrderRepository.findById(orderId)
                .map(order -> collectProductsExpiringBefore(order, order.getDeliveryDate()))
                .orElseGet(List::of);
    }

    private List<String> collectProductsExpiringBefore(ClientOrder order, LocalDate referenceDate) {
        if (referenceDate == null) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        for (OrderedProduct op : order.getOrderedProducts()) {
            Product product = op.getProduct();
            // Минимальный срок годности по партиям на складе, не истёкшим к referenceDate.
            // Если такой не нашёлся — значит к дате доставки все партии будут просрочены.
            Optional<LocalDate> earliestFresh = productExpirationService
                    .getEarliestUnexpiredExpiration(product, referenceDate);
            if (earliestFresh.isEmpty()) {
                result.add(product.getName() + " (" + product.getArticle()
                        + ", нет партий с непросроченным сроком к " + referenceDate.format(DISPLAY_DATE_FMT) + ")");
            }
        }
        return result;
    }

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

            for (OrderedProduct op : order.getOrderedProducts()) {
                if (!op.isFullyPicked()) {
                    log.error("Заказ №{}: позиция '{}' собрана не полностью ({}/{})",
                            order.getOrderNumber(), op.getProduct().getName(),
                            op.getPickedQuantity(), op.getQuantity());
                    return false;
                }
            }

            order.setStatus(ClientOrderStatus.READY);
            clientOrderRepository.save(order);

            String assemblyNotification = "Заказ №" + order.getOrderNumber()
                    + " готов к отгрузке. Клиент: " + order.getClient().getOrganizationName();
            notificationService.notifyByRole("ROLE_EMPLOYEE_MANAGER", assemblyNotification);
            notificationService.notifyByRole("ROLE_EMPLOYEE_WAREHOUSE_MANAGER", assemblyNotification);

            log.info("Заказ №{}: сборка завершена, готов к отгрузке", order.getOrderNumber());
            return true;

        } catch (Exception e) {
            log.error("Ошибка при завершении сборки заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== ПИК-ЛИНИИ СБОРКИ ==========

    @Transactional
    public String addPick(Long orderedProductId, Long supplyId, Long zoneId, int quantity) {
        try {
            if (quantity <= 0) return "Количество должно быть больше нуля";

            Optional<OrderedProduct> optOp = orderedProductRepository.findById(orderedProductId);
            if (optOp.isEmpty()) return "Позиция заказа не найдена";
            OrderedProduct op = optOp.get();

            ClientOrder order = op.getClientOrder();
            if (order.getStatus() != ClientOrderStatus.IN_PROGRESS) {
                return "Подбор возможен только в статусе «Сборка» (текущий: "
                        + order.getStatus().getDisplayName() + ")";
            }

            Optional<Supply> optSupply = supplyRepository.findById(supplyId);
            if (optSupply.isEmpty()) return "Партия не найдена";
            Supply supply = optSupply.get();
            if (!supply.getProduct().getId().equals(op.getProduct().getId())) {
                return "Партия не принадлежит товару позиции";
            }
            LocalDate deliveryDate = order.getDeliveryDate();
            if (supply.getExpirationDate() == null
                    || (deliveryDate != null && supply.getExpirationDate().isBefore(deliveryDate))) {
                return "Партия #" + supplyId + " истекает до даты доставки";
            }

            Optional<StorageZone> optZone = storageZoneRepository.findById(zoneId);
            if (optZone.isEmpty()) return "Зона не найдена";
            StorageZone zone = optZone.get();

            Optional<ZoneProduct> optZp = zoneProductRepository.findByZoneAndSupply(zone, supply);
            if (optZp.isEmpty()) return "Партия #" + supplyId + " не размещена в зоне '" + zone.getLabel() + "'";

            int onStock = optZp.get().getQuantity();
            int alreadyPicked = orderedProductPickRepository.sumQuantityByZoneAndSupply(zoneId, supplyId);
            int availableForPick = onStock - alreadyPicked;
            if (quantity > availableForPick) {
                return "В связке зона '" + zone.getLabel() + "' + партия #" + supplyId
                        + " доступно к подбору только " + availableForPick + " шт. "
                        + "(на складе: " + onStock + ", уже зарезервировано: " + alreadyPicked + ")";
            }

            int remainingForPosition = op.getQuantity() - op.getPickedQuantity();
            if (quantity > remainingForPosition) {
                return "Превышение количества по позиции: ещё нужно " + remainingForPosition + " шт.";
            }

            OrderedProductPick pick = new OrderedProductPick(op, supply, zone, quantity);
            op.getPicks().add(pick);
            orderedProductPickRepository.save(pick);

            log.info("Заказ №{}: добавлен пик товара '{}', партия #{}, зона '{}', кол-во {}",
                    order.getOrderNumber(), op.getProduct().getName(), supplyId, zone.getLabel(), quantity);
            return null;
        } catch (Exception e) {
            log.error("Ошибка при добавлении пика: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "Внутренняя ошибка при добавлении пика";
        }
    }

    @Transactional
    public String removePick(Long pickId) {
        try {
            Optional<OrderedProductPick> optPick = orderedProductPickRepository.findById(pickId);
            if (optPick.isEmpty()) return "Не найден";
            OrderedProductPick pick = optPick.get();
            ClientOrder order = pick.getOrderedProduct().getClientOrder();
            if (order.getStatus() != ClientOrderStatus.IN_PROGRESS) {
                return "Удаление пика возможно только в статусе «Сборка»";
            }
            pick.getOrderedProduct().getPicks().remove(pick);
            orderedProductPickRepository.delete(pick);
            log.info("Заказ №{}: удалён пик #{} (партия #{}, зона '{}', {} шт.)",
                    order.getOrderNumber(), pickId,
                    pick.getSupply().getId(), pick.getZone().getLabel(), pick.getQuantity());
            return null;
        } catch (Exception e) {
            log.error("Ошибка при удалении пика: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return "Внутренняя ошибка при удалении пика";
        }
    }

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

    /**
     * Полное удаление заказа администратором.
     * Разрешено только до отгрузки (NEW, CONFIRMED, RESERVED, CANCELLED).
     * При резервировании — освобождает резерв товара.
     * Удаляет файл договора, если он есть.
     */
    @Transactional
    public boolean deleteOrderByAdmin(Long orderId) {
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
                    && currentStatus != ClientOrderStatus.RESERVED
                    && currentStatus != ClientOrderStatus.CANCELLED) {
                log.error("Удаление заказа №{} невозможно — статус {} (уже в работе или отгружен)",
                        order.getOrderNumber(), currentStatus);
                return false;
            }

            // Освобождаем резерв
            if (currentStatus == ClientOrderStatus.RESERVED) {
                for (OrderedProduct op : order.getOrderedProducts()) {
                    Product product = op.getProduct();
                    product.setReservedQuantity(
                            Math.max(0, product.getReservedQuantity() - op.getQuantity()));
                    productRepository.save(product);
                }
                log.info("Заказ №{}: резервирование снято при удалении", order.getOrderNumber());
            }

            // Удаляем файл договора
            if (order.getContractFile() != null && !order.getContractFile().isBlank()) {
                try {
                    ContractService.deleteContract(order.getContractFile());
                } catch (Exception e) {
                    log.warn("Не удалось удалить файл договора '{}': {}", order.getContractFile(), e.getMessage());
                }
            }

            String orderNumber = order.getOrderNumber();
            clientOrderRepository.delete(order);

            log.info("Заказ №{} удалён администратором", orderNumber);
            return true;

        } catch (Exception e) {
            log.error("Ошибка при удалении заказа: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }
    }

    // ========== АНАЛИТИКА ==========

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
