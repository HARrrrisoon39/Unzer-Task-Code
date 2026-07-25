package com.unzer.shop.customer.service;

import com.unzer.shop.customer.model.Customer;
import com.unzer.shop.customer.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Transactional
    public String register(String email, String rawPassword) {
        if (customerRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already registered");
        }
        Customer customer = Customer.builder()
                .email(email)
                .passwordHash(passwordEncoder.encode(rawPassword))
                .role("CUSTOMER")
                .build();
        customerRepository.save(customer);
        return jwtService.generate(email, "CUSTOMER");
    }

    public String login(String email, String rawPassword) {
        Customer customer = customerRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException("Invalid credentials"));
        if (!passwordEncoder.matches(rawPassword, customer.getPasswordHash())) {
            throw new IllegalArgumentException("Invalid credentials");
        }
        return jwtService.generate(email, customer.getRole());
    }
}
