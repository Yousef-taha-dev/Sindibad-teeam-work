
package com.sindibad.App.config;

import com.sindibad.App.entity.Admin;
import com.sindibad.App.entity.Person;
import com.sindibad.App.repository.AdminRepository;
import com.sindibad.App.repository.PersonRepository;

import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@ConditionalOnProperty(
        name = "app.admin.bootstrap.enabled",
        havingValue = "true"
)
public class AdminSeeder implements CommandLineRunner {

    private final PersonRepository personRepository;
    private final AdminRepository adminRepository;

    public AdminSeeder(
            PersonRepository personRepository,
            AdminRepository adminRepository) {

        this.personRepository = personRepository;
        this.adminRepository = adminRepository;
    }

    @Override
    @Transactional
    public void run(String... args) {

        String email = System.getenv("ADMIN_EMAIL");

        if (email == null || email.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_EMAIL environment variable is required."
            );
        }

        Person person = personRepository.findByEmail(email)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "No user found with email: " + email
                        )
                );

        if (adminRepository.existsByPersonPersonId(
                person.getPersonId())) {

            System.out.println("This user is already an ADMIN.");
            return;
        }

        Admin admin = new Admin();
        admin.setPerson(person);

        adminRepository.save(admin);

        System.out.println(
                "User successfully promoted to ADMIN: " + email
        );
    }
}
