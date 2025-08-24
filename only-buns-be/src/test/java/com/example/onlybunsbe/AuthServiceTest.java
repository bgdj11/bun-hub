package com.example.onlybunsbe;

import com.example.onlybunsbe.DTO.UserRequest;
import com.example.onlybunsbe.exception.ResourceConflictException;
import com.example.onlybunsbe.model.User;
import com.example.onlybunsbe.repository.UserRepository;
import com.example.onlybunsbe.service.AuthService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class AuthServiceConcurrencyTest {

    @Autowired AuthService authService;
    @Autowired UserRepository userRepository;

    @BeforeEach
    void clean() {
        var u = userRepository.findByUsername("sameuser");
        if (u != null) userRepository.delete(u);
    }

    private static UserRequest req(String username) {
        UserRequest r = new UserRequest();
        r.setUsername(username);
        r.setPassword("12345678");
        r.setEmail(UUID.randomUUID() + "@example.com");
        r.setFirstname("TestIme");
        r.setLastname("TestPrezime");
        // popuni ostala obavezna polja iz UserRequest ako postoje
        return r;
    }

    @Test
    void concurrentRegistration_allowsOnlyOne() throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        AtomicInteger failures = new AtomicInteger();

        Runnable task = () -> {
            try {
                start.await(); // start obe niti istovremeno
                authService.register(req("sameuser"));
                success.incrementAndGet();
            } catch (ResourceConflictException ex) {
                failures.incrementAndGet();
            } catch (Exception ex) {
                // bilo koja druga greška ne bi smela da se desi
                fail("Unexpected exception: " + ex);
            }
        };

        Thread t1 = new Thread(task);
        Thread t2 = new Thread(task);
        t1.start(); t2.start();
        start.countDown();
        t1.join(); t2.join();

        // očekivanja:
        assertEquals(1, success.get(), "Tačno jedna registracija treba da uspe");
        assertEquals(1, failures.get(), "Tačno jedna registracija treba da padne zbog UNIQUE");

        // dodatna verifikacija stanja u bazi
        assertEquals(1, userRepository.countByUsername("sameuser"),
                "U bazi sme da postoji tačno jedan user sa tim username-om");

        User u = userRepository.findByUsername("sameuser");
        assertNotNull(u);
    }
}
