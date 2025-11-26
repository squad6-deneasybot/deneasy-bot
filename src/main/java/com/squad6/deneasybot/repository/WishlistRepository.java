package com.squad6.deneasybot.repository;

import com.squad6.deneasybot.model.Wishlist;
import com.squad6.deneasybot.model.WishlistDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface WishlistRepository extends JpaRepository<Wishlist, Long> {

    @Query("SELECT new com.squad6.deneasybot.model.WishlistDTO(w.id, w.content, w.createdAt, u.name, c.name) " +
            "FROM Wishlist w " +
            "JOIN w.user u " +
            "JOIN u.company c " +
            "ORDER BY w.createdAt DESC")
    List<WishlistDTO> findAllWithUserDetails();
}