package com.multigenesys.booking.config;

import com.multigenesys.booking.entity.Reservation;
import com.multigenesys.booking.entity.ReservationStatus;
import com.multigenesys.booking.entity.Resource;
import com.multigenesys.booking.entity.ResourceType;
import com.multigenesys.booking.entity.Role;
import com.multigenesys.booking.entity.User;
import com.multigenesys.booking.repository.ReservationRepository;
import com.multigenesys.booking.repository.ResourceRepository;
import com.multigenesys.booking.repository.UserRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Slf4j
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final ResourceRepository resourceRepository;
    private final ReservationRepository reservationRepository;
    private final PasswordEncoder passwordEncoder;
    private final String adminPassword;
    private final String userPassword;

    public DataInitializer(
            UserRepository userRepository,
            ResourceRepository resourceRepository,
            ReservationRepository reservationRepository,
            PasswordEncoder passwordEncoder,
            @Value("${application.seed.admin-password:Admin@123}") String adminPassword,
            @Value("${application.seed.user-password:User@123}") String userPassword) {
        this.userRepository = userRepository;
        this.resourceRepository = resourceRepository;
        this.reservationRepository = reservationRepository;
        this.passwordEncoder = passwordEncoder;
        this.adminPassword = adminPassword;
        this.userPassword = userPassword;
    }

    @Override
    public void run(String... args) {
        seedUsers();
        seedResourcesAndReservations();
    }

    private void seedUsers() {
        createAndSaveUser("admin", "admin@example.com", adminPassword, "System Administrator", Role.ROLE_ADMIN);
        createAndSaveUser("user1", "user1@example.com", userPassword, "John Doe", Role.ROLE_USER);
        createAndSaveUser("user2", "user2@example.com", userPassword, "Jane Smith", Role.ROLE_USER);
    }

    private void createAndSaveUser(String username, String email, String password, String fullName, Role role) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .email(email)
                    .password(passwordEncoder.encode(password))
                    .fullName(fullName)
                    .role(role)
                    .build();
            userRepository.save(user);
            log.info("Default {} user seeded: {} / {}", role.name(), username, email);
        }
    }

    private void seedResourcesAndReservations() {
        seedResources();
        seedReservations();
    }

    private void seedResources() {
        if (resourceRepository.count() == 0) {
            Resource conferenceRoom = Resource.builder()
                    .name("Grand Conference Room A")
                    .description("High-capacity conference room equipped with 4K projector, surround sound, and video conferencing.")
                    .type(ResourceType.CONFERENCE_HALL)
                    .pricePerHour(new BigDecimal("150.00"))
                    .isAvailable(true)
                    .build();

            Resource executiveVan = Resource.builder()
                    .name("Executive Shuttle Van")
                    .description("8-seater luxury electric shuttle for corporate transport.")
                    .type(ResourceType.VEHICLE)
                    .pricePerHour(new BigDecimal("85.00"))
                    .isAvailable(true)
                    .build();

            Resource studioCamera = Resource.builder()
                    .name("Sony FX3 Cinema Camera Kit")
                    .description("Full frame cinema camera with GM prime lenses and wireless audio kit.")
                    .type(ResourceType.EQUIPMENT)
                    .pricePerHour(new BigDecimal("45.00"))
                    .isAvailable(true)
                    .build();

            Resource hotDesk = Resource.builder()
                    .name("Dedicated Workspace Desk 12")
                    .description("Ergonomic sit-stand desk with dual 4K monitors in quiet zone.")
                    .type(ResourceType.DESK)
                    .pricePerHour(new BigDecimal("15.00"))
                    .isAvailable(true)
                    .build();

            resourceRepository.save(conferenceRoom);
            resourceRepository.save(executiveVan);
            resourceRepository.save(studioCamera);
            resourceRepository.save(hotDesk);
            log.info("Sample bookable resources seeded successfully.");
        }
    }

    private void seedReservations() {
        if (reservationRepository.count() == 0) {
            User user1 = userRepository.findByUsername("user1").orElse(null);
            Resource conferenceRoom = resourceRepository.findAll().stream().findFirst().orElse(null);

            if (user1 != null && conferenceRoom != null) {
                LocalDateTime start = LocalDateTime.now().plusDays(2).withHour(10).withMinute(0).withSecond(0).withNano(0);
                LocalDateTime end = start.plusHours(3);

                Reservation sampleReservation = Reservation.builder()
                        .user(user1)
                        .resource(conferenceRoom)
                        .startTime(start)
                        .endTime(end)
                        .totalPrice(new BigDecimal("450.00"))
                        .status(ReservationStatus.CONFIRMED)
                        .build();

                reservationRepository.save(sampleReservation);
                log.info("Sample reservation seeded for user1.");
            }
        }
    }
}
