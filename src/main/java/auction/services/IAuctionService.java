package auction.services;

import auction.model.Auction;
import auction.model.Bid;
import auction.model.User;

import java.math.BigDecimal;
import java.util.List;

public interface IAuctionService {
    List<Auction> getAllActiveAuctions();
    List<Auction> getActiveAuctionsWithUserBids(User user);
    Auction getAuctionById(Long id);
    void placeBid(Long auctionId, User bidder, BigDecimal amount);
    List<Bid> getBidsForAuction(Long auctionId);
    BigDecimal getSuggestedBid(Long auctionId);
    List<Auction> getAuctionsByCategory(String category);
    List<Auction> searchAuctions(String search);
    void createAuction(Auction auction);
    void buyNow(Long auctionId, User buyer);
    List<Auction> getFinishedAuctions();
    List<Auction> getUserAuctionsByCategory(User user, String category);
    List<Auction> searchUserAuctions(User user, String search);

    List<Auction> getUserPurchases(User user);

    List<Auction> getAuctionsBySeller(User seller);
    void updateAuction(Long id, Auction updatedAuction, User user);
    void removeAuction(Long id, User user);
    List<Auction> getArchivedAuctions();
}