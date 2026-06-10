package com.drivex;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DriverSeeder implements CommandLineRunner {

    private final DriverRepository driverRepo;

    public DriverSeeder(DriverRepository driverRepo) {
        this.driverRepo = driverRepo;
    }

    @Override
    public void run(String... args) {
        // Sirf tab seed karo jab collection empty ho
        if (driverRepo.count() > 0) return;

        // Bike drivers
        driverRepo.save(new Driver("Ali Raza",     "Bike"));
        driverRepo.save(new Driver("Hamza Khan",   "Bike"));
        driverRepo.save(new Driver("Usman Tariq",  "Bike"));
        driverRepo.save(new Driver("Habib Ullah",  "Bike"));
        driverRepo.save(new Driver("soniyo Tariq",  "Bike"));


        // Auto drivers
        driverRepo.save(new Driver("Bilal Ahmed",  "Auto"));
        driverRepo.save(new Driver("Kamran Shah",  "Auto"));
        driverRepo.save(new Driver("Sabi hussain",  "Auto"));
        driverRepo.save(new Driver("Hasseb Ahmed",  "Auto"));
        driverRepo.save(new Driver("Maheen Shah",  "Auto"));



        // Car drivers
        driverRepo.save(new Driver("Fahad Malik",  "Car"));
        driverRepo.save(new Driver("Zubair Ali",   "Car"));
        driverRepo.save(new Driver("Hunain Ali",   "Car"));
        driverRepo.save(new Driver("Sadia Khan",   "Car"));
        driverRepo.save(new Driver("Shahid Afridi", "Car"));


        // InterCity drivers
        driverRepo.save(new Driver("Nawaz Ahmed",  "InterCity"));
        driverRepo.save(new Driver("Rashid Mehmood","InterCity"));
        driverRepo.save(new Driver("Sadia Khan",   "InterCity"));

        System.out.println("✅ Drivers seeded successfully!");
    }
}