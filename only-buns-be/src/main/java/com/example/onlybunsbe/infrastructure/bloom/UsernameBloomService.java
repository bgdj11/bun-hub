package com.example.onlybunsbe.infrastructure.bloom;

import com.example.onlybunsbe.repository.UserRepository;
import com.google.common.hash.BloomFilter;
import com.google.common.hash.Funnels;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

@Service
public class UsernameBloomService {

    private final UserRepository userRepository;

    private BloomFilter<String> bloom;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();

    // opcionalno: fajl za persistenciju između restarta
    private final File snapshot = new File("bloom-username.snapshot");

    public UsernameBloomService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @PostConstruct
    void init() {
        // pokuša da učita snapshot sa diska radi bržeg starta
        if (snapshot.exists()) {
            try (InputStream in = new BufferedInputStream(new FileInputStream(snapshot))) {
                bloom = BloomFilter.readFrom(in, Funnels.stringFunnel(StandardCharsets.UTF_8));
                return;
            } catch (Exception ignore) {}
        }

        long userCount = userRepository.count();
        long expectedInsertions = Math.max(userCount + 50_000, 50_000); // buffer za rast
        double fpp = 0.01; // 1% false-positive

        bloom = BloomFilter.create(Funnels.stringFunnel(StandardCharsets.UTF_8),
                expectedInsertions, fpp);

        // napuni iz baze
        userRepository.findAll().forEach(u -> {
            String norm = normalize(u.getUsername());
            lock.writeLock().lock();
            try { bloom.put(norm); }
            finally { lock.writeLock().unlock(); }
        });
    }

    @PreDestroy
    void snapshot() {
        try (OutputStream out = new BufferedOutputStream(new FileOutputStream(snapshot))) {
            lock.readLock().lock();
            try { bloom.writeTo(out); }
            finally { lock.readLock().unlock(); }
        } catch (IOException ignore) {}
    }

    public boolean mightExist(String username) {
        String norm = normalize(username);
        lock.readLock().lock();
        try { return bloom.mightContain(norm); }
        finally { lock.readLock().unlock(); }
    }

    public void add(String username) {
        String norm = normalize(username);
        lock.writeLock().lock();
        try { bloom.put(norm); }
        finally { lock.writeLock().unlock(); }
    }

    private String normalize(String s) {
        // Ako username smatraš case-insensitive, normalizuj:
        return s == null ? "" : s.trim().toLowerCase();
    }
}
