package com.example.finance.dataseeder;

import com.example.finance.entity.*;
import com.example.finance.repo.*;
import com.example.finance.enums.RoleName;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.*;

@Component
public class DataSeeder implements CommandLineRunner {
    @Autowired
    private RoleRepo roleRepo;

    @Autowired
    private AuthorityRepo authorityRepo;

    @Autowired
    private NotificationRepo notificationRepo;

    @Autowired
    private UserRepo userRepo;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private CategoryIconRepo categoryIconRepo;

    @Override
    public void run(String... args) throws Exception {
        if(roleRepo.count() == 0){
            seedData();
            seedCategoryIcons();
        }
    }

    private void seedData(){
        List<String> authorityNames = List.of("CREATE",
                "UPDATE",
                "DELETE",
                "VIEW"
                );
        Map<String, Authority> authorityMap = new HashMap<>();

        //Seed Authority
        for(String name: authorityNames){
            Authority auth = new Authority();
            auth.setName(name);
            authorityMap.put(name, authorityRepo.save(auth));
        }

        //Seed ROLE USER
        Role userRole = new Role();
        userRole.setName(RoleName.USER);
        userRole.setAuthorities(Set.of(
                authorityMap.get("CREATE"),
                authorityMap.get("VIEW")
        ));

        roleRepo.save(userRole);

        //Seed ROlE ADMIN
        Role adminRole = new Role();
        adminRole.setName(RoleName.ADMIN);
        adminRole.setAuthorities(new HashSet<>(authorityMap.values()));

        roleRepo.save(adminRole);

        //Seed user
        if (!userRepo.existsByEmail("user@gmail.com")) {
            User user = User.builder()
                    .email("user@gmail.com")
                    .password(passwordEncoder.encode("user123"))
                    .roles(Set.of(userRole))
                    .build();

            Notification notification = new Notification();

            notification.setDailyReminder(false);
            notification.setBudgetAlert(true);
            notification.setTipsEnabled(false);
            notification.setUser(user);

            userRepo.save(user);
            notificationRepo.save(notification);
        }

        //Seed admin
        if (!userRepo.existsByEmail("admin@gmail.com")) {
            User admin = User.builder()
                    .email("admin@gmail.com")
                    .password(passwordEncoder.encode("admin123"))
                    .roles(Set.of(adminRole))
                    .build();

            Notification notification = new Notification();

            notification.setDailyReminder(false);
            notification.setBudgetAlert(true);
            notification.setTipsEnabled(false);
            notification.setUser(admin);

            userRepo.save(admin);
            notificationRepo.save(notification);
        }
    }

    private void seedCategoryIcons() {
        List<Object[]> defaultIcons = List.of(
                new Object[]{"Housing", "🏠", "https://cdn.example.com/icons/housing.png"},
                new Object[]{"Food", "🍽", "https://cdn.example.com/icons/food.png"},
                new Object[]{"Shopping", "🛍", "https://cdn.example.com/icons/shopping.png"},
                new Object[]{"Salary", "💵", "https://cdn.example.com/icons/salary.png"},
                new Object[]{"Freelance", "💼", "https://cdn.example.com/icons/freelance.png"},
                new Object[]{"Investments", "📈", "https://cdn.example.com/icons/investments.png"},
                new Object[]{"Other", "👤", "https://cdn.example.com/icons/other.png"},
                new Object[]{"Transportation", "🚗", "https://cdn.example.com/icons/transportation.png"},
                new Object[]{"Entertainment", "🎮", "https://cdn.example.com/icons/entertainment.png"},
                new Object[]{"Health", "🏥", "https://cdn.example.com/icons/health.png"},
                new Object[]{"Education", "🎓", "https://cdn.example.com/icons/education.png"},
                new Object[]{"Gifts", "🎁", "https://cdn.example.com/icons/gifts.png"}
        );

        for (Object[] icon : defaultIcons) {
            String name = (String) icon[0];
            String emoji = (String) icon[1];
            String iconUrl = (String) icon[2];

            // Kiểm tra xem đã tồn tại chưa
            if (!categoryIconRepo.existsByEmoji(emoji)) {
                CategoryIcon categoryIcon = new CategoryIcon();
                categoryIcon.setCategoryName(name);
                categoryIcon.setEmoji(emoji);
                categoryIcon.setIconUrl(iconUrl);
                categoryIconRepo.save(categoryIcon);
                System.out.println("  - Created category icon: " + name + " " + emoji);
            }
        }
    }
}
