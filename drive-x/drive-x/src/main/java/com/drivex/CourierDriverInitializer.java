package com.drivex;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class CourierDriverInitializer implements CommandLineRunner {

    private final CourierDriverRepository driverRepo;

    public CourierDriverInitializer(CourierDriverRepository driverRepo) {
        this.driverRepo = driverRepo;
    }

    @Override
    public void run(String... args) {
        // ✅ Sirf ek baar seed karo
        if (driverRepo.count() == 0) {
            driverRepo.save(new CourierDriver("Ali",   1));
            driverRepo.save(new CourierDriver("Bilal", 2));
            driverRepo.save(new CourierDriver("Kamran",3));
            driverRepo.save(new CourierDriver("Zain",  4));
            driverRepo.save(new CourierDriver("Hamza", 5));
            System.out.println("✅ 5 courier drivers initialized");
        }
    }
}