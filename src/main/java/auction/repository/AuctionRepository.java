package auction.repository;

import auction.model.Auction;
import auction.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuctionRepository extends JpaRepository<Auction, Long> {
    List<Auction> findByStatus(Auction.AuctionStatus status);

    @Query("SELECT DISTINCT a FROM Auction a JOIN a.bids b WHERE b.bidder = :user AND a.status = :status")
    List<Auction> findAuctionsWithUserBidsByStatus(@Param("user") User user, @Param("status") Auction.AuctionStatus status);
    List<Auction> findByStatusAndCategory(Auction.AuctionStatus status, String category);
    List<Auction> findByStatusAndTitleContainingIgnoreCase(Auction.AuctionStatus status, String search);
    List<Auction> findByStatusAndEndTimeBefore(Auction.AuctionStatus status, LocalDateTime endTime);
    List<Auction> findBySeller(User seller);
    List<Auction> findByStatusIn(List<Auction.AuctionStatus> statuses);
    List<Auction> findByWinnerAndStatus(User winner, Auction.AuctionStatus status);

}