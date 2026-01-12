package com.mai.siarsp.service.visitor;

import com.mai.siarsp.dto.VisitorDTO;
import com.mai.siarsp.mapper.VisitorMapper;
import com.mai.siarsp.models.Role;
import com.mai.siarsp.models.Visitor;
import com.mai.siarsp.repo.RoleRepository;
import com.mai.siarsp.repo.VisitorRepository;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.util.*;

@Service
@Getter
@Slf4j
public class VisitorService implements UserDetailsService {

    private final VisitorRepository visitorRepository;
    private final RoleRepository roleRepository;
    private final BCryptPasswordEncoder bCryptPasswordEncoder;

    public VisitorService(VisitorRepository visitorRepository, RoleRepository roleRepository, BCryptPasswordEncoder bCryptPasswordEncoder) {
        this.visitorRepository = visitorRepository;
        this.roleRepository = roleRepository;
        this.bCryptPasswordEncoder = bCryptPasswordEncoder;
    }


    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return visitorRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("Не найден посетитель с username = " + username + "."));
    }

    @Transactional
    public boolean saveVisitor(Visitor visitor) {
        log.info("Начинаем сохранять посетителя с username = {}...", visitor.getUsername());

        if (checkUserName(visitor.getUsername())) {
            log.error("Посетитель с username = {} уже существует. Используйте другой username.", visitor.getUsername());
            return false;
        }

        Optional<Role> roleOptional = roleRepository.findByName("ROLE_VISITOR");

        if (roleOptional.isEmpty()) {
            log.error("Роль ROLE_VISITOR не найдена. Невозможно создать посетителя.");
            return false;
        }

        visitor.setRole(roleOptional.get());
        visitor.setPassword(bCryptPasswordEncoder.encode(visitor.getPassword()));

        try {
            visitorRepository.save(visitor);
        } catch (Exception e) {
            log.error("Ошибка при сохранении посетителя {}: {}", visitor.getUsername(), e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Посетитель с username = {} успешно сохранён.", visitor.getUsername());
        return true;
    }

    @Transactional
    public boolean editVisitor(Long id,
                               String inputLastName, String inputFirstName,
                               String inputPatronymicName, String inputSex,
                               LocalDate dateBirthday,
                               String inputUsername,
                               String inputMobileNumber,
                               boolean isUsernameChanged) {

        Optional<Visitor> visitorOptional = visitorRepository.findById(id);

        if (visitorOptional.isEmpty()) {
            log.error("Не найден посетитель с id = {}...", id);
            return false;
        }

        if (isUsernameChanged && visitorRepository.existsByUsernameAndIdNot(inputUsername, id)) {
            log.error("Посетитель с username = {} уже существует. Используйте другой username.", inputUsername);
            return false;
        }

        Visitor visitor = visitorOptional.get();
        log.info("Обновление данных посетителя с id = {}...", id);

        visitor.setLastName(inputLastName);
        visitor.setFirstName(inputFirstName);
        visitor.setPatronymicName(inputPatronymicName);
        visitor.setSex(inputSex);
        visitor.setDateBirthday(dateBirthday);
        visitor.setUsername(inputUsername);
        visitor.setMobileNumber(inputMobileNumber);

        try {
            visitorRepository.save(visitor);
        } catch (Exception e) {
            log.error("Ошибка при сохранении изменений посетителя: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Изменения для посетителя успешно сохранены.");
        return true;
    }


    @Transactional
    public boolean resetVisitorPassword(long id, String newPassword) {
        Optional<Visitor> visitorOptional = visitorRepository.findById(id);

        if (visitorOptional.isEmpty()) {
            log.error("Не найден посетитель с id = {}...", id);
            return false;
        }

        Visitor visitor = visitorOptional.get();

        log.info("Начинаем обновление пароля для посетителя с username = {}...", visitor.getUsername());

        visitor.setPassword(bCryptPasswordEncoder.encode(newPassword));
        visitor.setNeedChangePass(true);

        try {
            visitorRepository.save(visitor);
        } catch (Exception e) {
            log.error("Ошибка при обновлении пароля посетителя: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Пароль для посетителя успешно обновлён.");
        return true;
    }

    @Transactional
    public boolean deleteVisitor(long id) {
        Optional<Visitor> visitorOptional = visitorRepository.findById(id);

        if (visitorOptional.isEmpty()) {
            log.error("Не найден посетитель с id = {}...", id);
            return false;
        }

        Visitor visitor = visitorOptional.get();

        log.info("Начинаем удаление посетителя с username = {}...", visitor.getUsername());

        try {
            visitorRepository.delete(visitor);
        } catch (Exception e) {
            log.error("Ошибка при удалении посетителя: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Посетитель успешно удалён.");
        return true;
    }

    public boolean checkUserName(String username) {
        return visitorRepository.existsByUsername(username);
    }

    @Transactional
    public boolean accountVisitorLocked(Long id) {
        Optional<Visitor> visitorOptional = visitorRepository.findById(id);

        if (visitorOptional.isEmpty()) {
            log.error("Не найден посетитель с id = {}...", id);
            return false;
        }

        Visitor visitor = visitorOptional.get();
        log.info("Начинаем блокировку аккаунта посетителя с username = {}...", visitor.getUsername());
        visitor.setActive(false);

        try {
            visitorRepository.save(visitor);
        } catch (Exception e) {
            log.error("Ошибка при блокировке аккаунта посетителя: {}", e.getMessage(), e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return false;
        }

        log.info("Аккаунт посетителя успешно заблокирован.");
        return true;
    }

    public List<VisitorDTO> getAllVisitors() {
        List<Visitor> visitors = visitorRepository.findAll()
                .stream().filter(e -> !e.getId().equals(getAuthenticationVisitorDTO().getId()))
                .toList();
        return VisitorMapper.INSTANCE.toDTOList(visitors);
    }


    public VisitorDTO getAuthenticationVisitorDTO() {
        Optional<Visitor> visitorOptional = visitorRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        return visitorOptional.map(VisitorMapper.INSTANCE::toDTO).orElse(null);
    }

    public boolean changePassword(String newPassword) {
        Optional<Visitor> visitorOptional = visitorRepository.findByUsername(
                SecurityContextHolder.getContext().getAuthentication().getName());
        if (visitorOptional.isPresent()) {
            Visitor visitor = visitorOptional.get();
            log.info("Новый пароль - " + newPassword);
            visitor.setPassword(bCryptPasswordEncoder.encode(newPassword));
            visitor.setNeedChangePass(false);
            visitorRepository.save(visitor);
            return true;
        }
        return false;
    }

}
