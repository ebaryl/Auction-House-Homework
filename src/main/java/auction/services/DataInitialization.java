package auction.services;

import auction.model.Auction;
import auction.model.User;
import auction.repository.AuctionRepository;
import auction.dao.impl.spring.data.UserDAO;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DataInitialization implements CommandLineRunner {

    private final UserDAO userDAO;
    private final AuctionRepository auctionRepository;

    @Override
    public void run(String... args) throws Exception {
        User janusz = new User(null, "Janusz", "Kowalski",
                "janusz", DigestUtils.md5DigestAsHex("janusz123".getBytes()), User.Role.USER);
        User wiesiek = new User(null, "Wiesiek", "Admin",
                "wiesiek", DigestUtils.md5DigestAsHex("wiesiek123".getBytes()), User.Role.ADMIN);
        User admin = new User(null, "admin", "admin",
                "admin", DigestUtils.md5DigestAsHex("admin".getBytes()), User.Role.ADMIN);

        List<User> users = Arrays.asList(janusz, wiesiek, admin);
        userDAO.saveAll(users);

        List<Auction> auctions = Arrays.asList(
                createAuction("Luxury Apartment", "Spacious 3-bedroom apartment with a view", new BigDecimal("500000.00"), new BigDecimal("550000.00"), janusz, "Real Estate"),
                createAuction("Vintage Car", "1965 Ford Mustang in excellent condition", new BigDecimal("35000.00"), new BigDecimal("40000.00"), wiesiek, "Automotive"),
                createAuction("Antique Vase", "18th century Chinese porcelain vase", new BigDecimal("5000.00"), new BigDecimal("7000.00"), admin, "Art and Antiques"),
                createAuction("Designer Handbag", "Limited edition Louis Vuitton bag", new BigDecimal("2000.00"), new BigDecimal("2500.00"), janusz, "Fashion and Accessories"),
                createAuction("Rare Comic Book", "First edition Superman comic", new BigDecimal("50000.00"), new BigDecimal("60000.00"), wiesiek, "Collectibles"),
                createAuction("Diamond Necklace", "18k gold necklace with 2 carat diamond pendant", new BigDecimal("15000.00"), new BigDecimal("18000.00"), admin, "Jewelry")
        );

        auctionRepository.saveAll(auctions);

        System.out.println("Sample data initialized successfully.");
    }

    private Auction createAuction(String title, String description, BigDecimal startingPrice, BigDecimal buyNowPrice, User seller, String category) {
        return Auction.builder()
                .title(title)
                .description(description)
                .startingPrice(startingPrice)
                .buyNowPrice(buyNowPrice)
                .startTime(LocalDateTime.now())
                .endTime(LocalDateTime.now().plusDays(7))
                .seller(seller)
                .status(Auction.AuctionStatus.ACTIVE)
                .category(category)
                .build();
    }
}