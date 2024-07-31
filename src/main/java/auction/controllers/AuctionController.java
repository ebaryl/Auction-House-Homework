package auction.controllers;

import auction.model.Auction;
import auction.model.User;
import auction.services.IAuctionService;
import auction.session.SessionConstants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Arrays;

@Controller
@RequestMapping("/auctions")
public class                AuctionController {

    private final IAuctionService auctionService;

    @Autowired
    public AuctionController(IAuctionService auctionService) {
        this.auctionService = auctionService;
    }

    @GetMapping
    public String listAllAuctions(Model model,
                                  @RequestParam(required = false) String category,
                                  @RequestParam(required = false) String sort,
                                  @RequestParam(required = false) String search,
                                  HttpServletRequest request,
                                  HttpSession session) {
        List<String> categories = Arrays.asList("All", "Real Estate", "Automotive", "Art and Antiques",
                "Fashion and Accessories", "Collectibles", "Jewelry");
        model.addAttribute("categories", categories);
        model.addAttribute("currentUrl", request.getRequestURI());
        model.addAttribute("currentCategory", category != null ? category : "All");
        model.addAttribute("currentSort", sort != null ? sort : "");

        List<Auction> auctions;
        if ("All".equals(category) || category == null) {
            auctions = auctionService.getAllActiveAuctions();
        } else {
            auctions = auctionService.getAuctionsByCategory(category);
        }

        if (search != null && !search.isEmpty()) {
            auctions = auctionService.searchAuctions(search);
        }

        if (sort != null) {
            switch (sort) {
                case "price_asc":
                    auctions.sort((a1, a2) -> a1.getCurrentBid().compareTo(a2.getCurrentBid()));
                    break;
                case "price_desc":
                    auctions.sort((a1, a2) -> a2.getCurrentBid().compareTo(a1.getCurrentBid()));
                    break;
                case "time_asc":
                    auctions.sort((a1, a2) -> a1.getEndTime().compareTo(a2.getEndTime()));
                    break;
                case "time_desc":
                    auctions.sort((a1, a2) -> a2.getEndTime().compareTo(a1.getEndTime()));
                    break;
                case "popularity":
                    auctions.sort((a1, a2) -> Integer.compare(a2.getBids().size(), a1.getBids().size()));
                    break;
            }
        }

        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        boolean isAdmin = user != null && user.getRole() == User.Role.ADMIN;
        model.addAttribute("isAdmin", isAdmin);

        model.addAttribute("auctions", auctions);

        return "auction-list";
    }

    @GetMapping("/archive")
    public String listArchiveAuctions(Model model) {
        List<Auction> archivedAuctions = auctionService.getArchivedAuctions();
        model.addAttribute("auctions", archivedAuctions);
        return "archive-auctions";
    }

    @GetMapping("/create")
    public String showCreateAuctionForm(Model model, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        model.addAttribute("auction", new Auction());
        return "create-auction";
    }
    Logger logger = LoggerFactory.getLogger(AuctionController.class);
    @PostMapping("/create")
    public String createAuction(@Valid @ModelAttribute Auction auction, BindingResult bindingResult, HttpSession session, Model model) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "create-auction";
        }
        auction.setStartTime(LocalDateTime.now());
        auction.setSeller(user);
        try {
            auctionService.createAuction(auction);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "create-auction";
        }
        return "redirect:/auctions";
    }

    @PostMapping("/{id}/buy")
    public String buyNow(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        auctionService.buyNow(id, user);
        return "redirect:/auctions/" + id;
    }
    @GetMapping("/my-bids")
    public String listUserBiddingAuctions(Model model, HttpSession session,
                                          @RequestParam(required = false) String category,
                                          @RequestParam(required = false) String search,
                                          HttpServletRequest request) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }

        List<String> categories = List.of("Real Estate", "Automotive", "Art and Antiques",
                "Fashion and Accessories", "Collectibles", "Jewelry");
        model.addAttribute("categories", categories);
        model.addAttribute("currentUrl", request.getRequestURI());

        List<Auction> auctions;
        if (category != null && !category.isEmpty()) {
            auctions = auctionService.getUserAuctionsByCategory(user, category);
        } else if (search != null && !search.isEmpty()) {
            auctions = auctionService.searchUserAuctions(user, search);
        } else {
            auctions = auctionService.getActiveAuctionsWithUserBids(user);
        }

        model.addAttribute("auctions", auctions);
        return "auction-list";
    }

    @GetMapping("/{id}")
    public String getAuction(@PathVariable Long id, Model model, HttpSession session) {
        Auction auction = auctionService.getAuctionById(id);
        model.addAttribute("auction", auction);
        model.addAttribute("bids", auctionService.getBidsForAuction(id));

        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user != null) {
            model.addAttribute("suggestedBid", auctionService.getSuggestedBid(id));
        }

        return "auction-details";
    }

    @PostMapping("/{id}/bid")
    public String placeBid(@PathVariable Long id, @RequestParam BigDecimal amount, HttpSession session, Model model) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        try {
            auctionService.placeBid(id, user, amount);
        } catch (RuntimeException e) {
            model.addAttribute("error", e.getMessage());
            return getAuction(id, model, session);
        }
        return "redirect:/auctions/" + id;
    }

    @GetMapping("/my-auctions")
    public String listMyAuctions(Model model, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        List<Auction> myAuctions = auctionService.getAuctionsBySeller(user);
        model.addAttribute("auctions", myAuctions);
        return "my-auctions";
    }
    @GetMapping("/edit/{id}")
    public String showEditAuctionForm(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        Auction auction = auctionService.getAuctionById(id);
        if (!auction.getSeller().getId().equals(user.getId())) {
            return "redirect:/auctions";
        }

        model.addAttribute("auction", auction);
        return "edit-auction";
    }
    @PostMapping("/edit/{id}")
    public String editAuction(@PathVariable Long id, @Valid @ModelAttribute Auction updatedAuction, BindingResult bindingResult, HttpSession session, Model model) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }

        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "edit-auction";
        }

        try {
            auctionService.updateAuction(id, updatedAuction, user);
        } catch (IllegalArgumentException e) {
            model.addAttribute("error", e.getMessage());
            return "edit-auction";
        }
        return "redirect:/auctions/my-auctions";
    }

    @PostMapping("/remove/{id}")
    public String removeAuction(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        auctionService.removeAuction(id, user);
        return "redirect:/auctions/my-auctions";
    }

    @PostMapping("/admin/remove/{id}")
    public String adminRemoveAuction(@PathVariable Long id, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/auctions";
        }
        auctionService.removeAuction(id, user);
        return "redirect:/auctions";
    }

    @GetMapping("/admin/edit/{id}")
    public String showAdminEditAuctionForm(@PathVariable Long id, Model model, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/auctions";
        }
        Auction auction = auctionService.getAuctionById(id);
        model.addAttribute("auction", auction);
        return "edit-auction";
    }

    @PostMapping("/admin/edit/{id}")
    public String adminEditAuction(@PathVariable Long id, @Valid @ModelAttribute Auction updatedAuction, BindingResult bindingResult,  HttpSession session, Model model) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        logger.error("ID is set to " + updatedAuction.getId());
        if (user == null || user.getRole() != User.Role.ADMIN) {
            return "redirect:/auctions";
        }
        if (bindingResult.hasErrors()) {
            model.addAttribute("errors", bindingResult.getAllErrors());
            return "redirect:/admin/edit/" + id;
        }
        auctionService.updateAuction(id, updatedAuction, user);
        return "redirect:/auctions";
    }

    @GetMapping("/my-purchases")
    public String listMyPurchases(Model model, HttpSession session) {
        User user = (User) session.getAttribute(SessionConstants.USER_KEY);
        if (user == null) {
            return "redirect:/login";
        }
        List<Auction> purchases = auctionService.getUserPurchases(user);
        model.addAttribute("purchases", purchases);
        return "my-purchases";
    }
}