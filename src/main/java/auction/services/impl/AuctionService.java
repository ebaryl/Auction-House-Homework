package auction.services.impl;

import auction.model.Auction;
import auction.model.Bid;
import auction.model.User;
import auction.repository.AuctionRepository;
import auction.repository.BidRepository;
import auction.services.IAuctionService;
import auction.validators.AuctionValidator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.Errors;
import org.springframework.validation.ValidationUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AuctionService implements IAuctionService {

    private final AuctionRepository auctionRepository;
    private final BidRepository bidRepository;
    private final AuctionValidator auctionValidator;

    @Autowired
    public AuctionService(AuctionRepository auctionRepository, BidRepository bidRepository, AuctionValidator auctionValidator) {
        this.auctionRepository = auctionRepository;
        this.bidRepository = bidRepository;
        this.auctionValidator = auctionValidator;
    }

    @Override
    public List<Auction> getAllActiveAuctions() {
        return auctionRepository.findByStatus(Auction.AuctionStatus.ACTIVE);
    }

    @Override
    public List<Auction> getActiveAuctionsWithUserBids(User user) {
        return auctionRepository.findAuctionsWithUserBidsByStatus(user, Auction.AuctionStatus.ACTIVE);
    }

    @Override
    public Auction getAuctionById(Long id) {
        return auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found with id: " + id));
    }

    @Override
    public List<Auction> getUserAuctionsByCategory(User user, String category) {
        return getActiveAuctionsWithUserBids(user).stream()
                .filter(auction -> auction.getCategory().equals(category))
                .collect(Collectors.toList());
    }

    @Override
    public List<Auction> searchUserAuctions(User user, String search) {
        return getActiveAuctionsWithUserBids(user).stream()
                .filter(auction -> auction.getTitle().toLowerCase().contains(search.toLowerCase()))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void placeBid(Long auctionId, User bidder, BigDecimal amount) {
        Auction auction = getAuctionById(auctionId);

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE || auction.getEndTime().isBefore(LocalDateTime.now())) {
            throw new RuntimeException("This auction is not active");
        }

        if (amount.compareTo(auction.getStartingPrice()) < 0) {
            throw new RuntimeException("Bid amount must be greater than or equal to the starting price");
        }

        List<Bid> existingBids = getBidsForAuction(auctionId);
        if (!existingBids.isEmpty() && amount.compareTo(existingBids.get(0).getAmount()) <= 0) {
            throw new RuntimeException("Bid amount must be greater than the current highest bid");
        }

        Bid newBid = Bid.builder()
                .auction(auction)
                .bidder(bidder)
                .amount(amount)
                .bidTime(LocalDateTime.now())
                .build();

        bidRepository.save(newBid);
        auction.getBids().add(newBid);
        auctionRepository.save(auction);
    }

    @Override
    public List<Bid> getBidsForAuction(Long auctionId) {
        return bidRepository.findByAuctionIdOrderByAmountDesc(auctionId);
    }

    @Override
    public BigDecimal getSuggestedBid(Long auctionId) {
        Auction auction = getAuctionById(auctionId);
        List<Bid> bids = getBidsForAuction(auctionId);

        if (bids.isEmpty()) {
            return auction.getStartingPrice().add(BigDecimal.ONE);
        } else {
            return bids.get(0).getAmount().add(BigDecimal.ONE);
        }
    }

    @Override
    public List<Auction> getAuctionsByCategory(String category) {
        return auctionRepository.findByStatusAndCategory(Auction.AuctionStatus.ACTIVE, category);
    }

    @Override
    public List<Auction> searchAuctions(String search) {
        return auctionRepository.findByStatusAndTitleContainingIgnoreCase(Auction.AuctionStatus.ACTIVE, search);
    }

    @Override
    public void createAuction(Auction auction) {
        Errors errors = new BeanPropertyBindingResult(auction, "auction");
        ValidationUtils.invokeValidator(auctionValidator, auction, errors);

        if (errors.hasErrors()) {
            throw new IllegalArgumentException("Invalid auction data: " + errors.getAllErrors());
        }
        auction.setStartTime(LocalDateTime.now(ZoneId.of("Europe/Warsaw")));
        auction.setStatus(Auction.AuctionStatus.ACTIVE);
        auctionRepository.save(auction);
    }

    @Override
    public List<Auction> getFinishedAuctions() {
        return auctionRepository.findByStatus(Auction.AuctionStatus.FINISHED);
    }

    @Override
    @Transactional
    public void buyNow(Long auctionId, User buyer) {
        Auction auction = auctionRepository.findById(auctionId)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (auction.getStatus() != Auction.AuctionStatus.ACTIVE) {
            throw new RuntimeException("This auction is not active");
        }

        auction.setStatus(Auction.AuctionStatus.FINISHED);
        auction.setWinner(buyer);
        auction.setEndTime(LocalDateTime.now());

        Bid buyNowBid = Bid.builder()
                .auction(auction)
                .bidder(buyer)
                .amount(auction.getBuyNowPrice())
                .bidTime(LocalDateTime.now())
                .build();

        bidRepository.save(buyNowBid);
        auction.getBids().add(buyNowBid);
        auctionRepository.save(auction);
    }

    @Scheduled(fixedRate = 60000) // Run every minute
    @Transactional
    public void closeExpiredAuctions() {
        List<Auction> expiredAuctions = auctionRepository.findByStatusAndEndTimeBefore(
                Auction.AuctionStatus.ACTIVE, LocalDateTime.now());

        for (Auction auction : expiredAuctions) {
            auction.setStatus(Auction.AuctionStatus.FINISHED);
            if (!auction.getBids().isEmpty()) {
                Bid winningBid = auction.getBids().stream()
                        .max((b1, b2) -> b1.getAmount().compareTo(b2.getAmount()))
                        .orElseThrow(() -> new RuntimeException("No winning bid found"));
                auction.setWinner(winningBid.getBidder());
            }
            auctionRepository.save(auction);
        }
    }

    @Override
    public List<Auction> getUserPurchases(User user) {
        return auctionRepository.findByWinnerAndStatus(user, Auction.AuctionStatus.FINISHED);
    }

    @Override
    public List<Auction> getAuctionsBySeller(User seller) {
        return auctionRepository.findBySeller(seller);
    }

    @Override
    public void updateAuction(Long id, Auction updatedAuction, User user) {
        Auction existingAuction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (!existingAuction.getSeller().getId().equals(user.getId()) && user.getRole() != User.Role.ADMIN) {
            throw new RuntimeException("You don't have permission to edit this auction");
        }

        Errors errors = new BeanPropertyBindingResult(updatedAuction, "auction");

        if (errors.hasErrors()) {
            throw new IllegalArgumentException("Invalid auction data: " + errors.getAllErrors());
        }

        existingAuction.setTitle(updatedAuction.getTitle());
        existingAuction.setDescription(updatedAuction.getDescription());
        existingAuction.setBuyNowPrice(updatedAuction.getBuyNowPrice());
        existingAuction.setCategory(updatedAuction.getCategory());
        existingAuction.setEndTime(updatedAuction.getEndTime());

        ValidationUtils.invokeValidator(auctionValidator, existingAuction, errors);

        auctionRepository.save(existingAuction);
    }

    @Override
    @Transactional
    public void removeAuction(Long id, User user) {
        Auction auction = auctionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Auction not found"));

        if (user.getRole() != User.Role.ADMIN && !auction.getSeller().getId().equals(user.getId())) {
            throw new RuntimeException("You don't have permission to remove this auction");
        }

        if (user.getRole() != User.Role.ADMIN && !auction.getBids().isEmpty()) {
            throw new RuntimeException("Cannot remove auction with existing bids");
        }

        auction.setStatus(Auction.AuctionStatus.REMOVED);
        auctionRepository.save(auction);
    }

    @Override
    public List<Auction> getArchivedAuctions() {
        return auctionRepository.findByStatusIn(List.of(Auction.AuctionStatus.FINISHED, Auction.AuctionStatus.REMOVED));
    }
}