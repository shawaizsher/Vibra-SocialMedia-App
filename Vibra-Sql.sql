-- ========================
-- USER TABLES & EXTENSIONS
-- ========================
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(45) NOT NULL,
    email VARCHAR(45),
    bio VARCHAR(45),
    profile_picture LONGBLOB,
    last_active DATETIME DEFAULT CURRENT_TIMESTAMP
);

-- ========================
-- FRIENDSHIP SYSTEM
-- ========================

-- Friend requests
CREATE TABLE friend_requests (
    request_id INT AUTO_INCREMENT PRIMARY KEY,
    sender_id INT NOT NULL,
    receiver_id INT NOT NULL,
    status VARCHAR(20) DEFAULT 'pending', -- 'pending', 'accepted', 'declined'
    sent_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(user_id),
    FOREIGN KEY (receiver_id) REFERENCES users(user_id),
    UNIQUE (sender_id, receiver_id)
);

-- Flat friends table (for direct lookup)
CREATE TABLE friends (
    user_id INT NOT NULL,
    friend_id INT NOT NULL,
    PRIMARY KEY (user_id, friend_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE,
    FOREIGN KEY (friend_id) REFERENCES users(user_id) ON DELETE CASCADE
);

-- ========================
-- BLOCK SYSTEM
-- ========================

-- Blocked users with timestamps
CREATE TABLE blocked_users (
    block_id INT AUTO_INCREMENT PRIMARY KEY,
    blocker_id INT NOT NULL,
    blocked_id INT NOT NULL,
    blocked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (blocker_id, blocked_id),
    FOREIGN KEY (blocker_id) REFERENCES users(user_id),
    FOREIGN KEY (blocked_id) REFERENCES users(user_id)
);


-- ========================
-- POSTS & INTERACTIONS
-- ========================
CREATE TABLE posts (
    post_id INT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    content TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    likeCount INT DEFAULT 0,
    media_type VARCHAR(10),  -- e.g., 'image', 'video'
    media LONGBLOB,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Ensure post_id starts at 4000
ALTER TABLE posts AUTO_INCREMENT = 4000;

-- Post comments
CREATE TABLE post_comments (
    comment_id INT PRIMARY KEY AUTO_INCREMENT,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(post_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Post likes
CREATE TABLE post_likes (
    like_id INT AUTO_INCREMENT PRIMARY KEY,
    post_id INT NOT NULL,
    user_id INT NOT NULL,
    liked_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (post_id) REFERENCES posts(post_id),
    FOREIGN KEY (user_id) REFERENCES users(user_id),
    UNIQUE (post_id, user_id)
);

-- ========================
-- MESSAGES
-- ========================
CREATE TABLE messages (
    id INT PRIMARY KEY AUTO_INCREMENT,
    sender_id INT,
    receiver_id INT,
    message_text TEXT,
    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (sender_id) REFERENCES users(user_id),
    FOREIGN KEY (receiver_id) REFERENCES users(user_id)
);

-- ========================
-- NOTIFICATIONS
-- ========================
CREATE TABLE notifications (
    notification_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NOT NULL, -- The recipient
    message TEXT NOT NULL,
    seen BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);
-- ========================
-- USER_SETTINGS
-- ========================

CREATE TABLE user_settings (
    user_id INT PRIMARY KEY,
    is_private BOOLEAN DEFAULT FALSE,
    read_receipts BOOLEAN DEFAULT TRUE,
    online_status BOOLEAN DEFAULT TRUE,
    message_permission VARCHAR(20) DEFAULT 'Everyone' CHECK (
        message_permission IN ('Everyone', 'Friends Only', 'No One')
    ),
    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
);

ALTER TABLE posts AUTO_INCREMENT = 4000;
ALTER TABLE posts MODIFY post_id INT NOT NULL AUTO_INCREMENT;



INSERT INTO friends (user_id, friend_id) VALUES (1, 4);
INSERT INTO friends (user_id, friend_id) VALUES (4, 1);


DROP TABLE IF EXISTS post_likes;
DROP TABLE IF EXISTS post_comments;

DESCRIBE post_comments;


ALTER TABLE post_comments ADD COLUMN created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP;




