
package com.love.website;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface LoveMessageRepository
        extends JpaRepository<LoveMessage, Long> {

    List<LoveMessage> findAllByPrivateShareFalseOrderByIdDesc();
}
