package com.vynk.demo;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Optional, idempotent fictional demo data. Disabled unless explicitly enabled. */
@Component
public class DemoDataInitializer implements ApplicationRunner {
    private final JdbcTemplate jdbc;
    private final PasswordEncoder passwordEncoder;
    private final boolean enabled;

    public DemoDataInitializer(JdbcTemplate jdbc, PasswordEncoder passwordEncoder,
                               @Value("${vynk.demo-data.enabled:false}") boolean enabled) {
        this.jdbc = jdbc;
        this.passwordEncoder = passwordEncoder;
        this.enabled = enabled;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!enabled) return;
        String[] names = {"Avery Chen", "Jordan Rivera", "Morgan Patel"};
        String[] usernames = {"averychen", "jordanrivera", "morganpatel"};
        long[] users = new long[3];
        for (int i = 0; i < usernames.length; i++) {
            Long existing = jdbc.query("SELECT id FROM user_account WHERE username_lower = ?", rs -> rs.next() ? rs.getLong(1) : null, usernames[i]);
            if (existing == null) {
                jdbc.update("INSERT INTO user_account(username,password_hash,status) VALUES(?,?,'ACTIVE')", usernames[i], passwordEncoder.encode("Demo-Vynk-2026!"));
                existing = jdbc.queryForObject("SELECT id FROM user_account WHERE username_lower = ?", Long.class, usernames[i]);
            }
            users[i] = existing;
            if (jdbc.queryForObject("SELECT COUNT(*) FROM profile WHERE user_id = ?", Integer.class, existing) == 0) {
                String[] parts = names[i].split(" ");
                jdbc.update("INSERT INTO profile(user_id,first_name,last_name,headline,summary,location) VALUES(?,?,?,?,?,?)",
                    existing, parts[0], parts[1], new String[]{"Product designer", "Software engineer", "Data analyst"}[i],
                    new String[]{"Designing clear, accessible products for people.", "Building reliable services and thoughtful developer experiences.", "Turning complex data into useful decisions."}[i],
                    new String[]{"Portland, OR", "Austin, TX", "Toronto, ON"}[i]);
            }
            Long profile = jdbc.queryForObject("SELECT id FROM profile WHERE user_id = ?", Long.class, existing);
            if (jdbc.queryForObject("SELECT COUNT(*) FROM profile_skill WHERE profile_id = ?", Integer.class, profile) == 0)
                jdbc.update("INSERT INTO profile_skill(profile_id,name,sort_order) VALUES(?,?,0)", profile, new String[]{"Product design", "Java", "Analytics"}[i]);
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM post WHERE author_id IN (?,?,?)", Integer.class, users[0], users[1], users[2]) == 0) {
            jdbc.update("INSERT INTO post(author_id,content) VALUES(?,?)", users[0], "Excited to share a few lessons from building accessible products: start by listening.");
            jdbc.update("INSERT INTO post(author_id,content) VALUES(?,?)", users[1], "A small reminder that good software is a team sport. What are you learning this week?");
            jdbc.update("INSERT INTO post(author_id,content) VALUES(?,?)", users[2], "Recently exploring better ways to make data stories understandable and actionable.");
        }
        connect(users[0], users[1]);
        Long conversation = jdbc.query("SELECT id FROM conversation WHERE user_low_id=? AND user_high_id=?", rs -> rs.next() ? rs.getLong(1) : null, Math.min(users[0], users[1]), Math.max(users[0], users[1]));
        if (conversation == null) {
            jdbc.update("INSERT INTO conversation(user_low_id,user_high_id) VALUES(?,?)", Math.min(users[0], users[1]), Math.max(users[0], users[1]));
            conversation = jdbc.queryForObject("SELECT id FROM conversation WHERE user_low_id=? AND user_high_id=?", Long.class, Math.min(users[0], users[1]), Math.max(users[0], users[1]));
        }
        if (jdbc.queryForObject("SELECT COUNT(*) FROM conversation_message WHERE conversation_id=?", Integer.class, conversation) == 0)
            jdbc.update("INSERT INTO conversation_message(conversation_id,sender_id,body) VALUES(?,?,?)", conversation, users[0], "Hi Jordan! Thanks for connecting. Looking forward to exchanging ideas.");
        for (long user : new long[]{users[0], users[1]})
            jdbc.update("MERGE INTO conversation_user_state(conversation_id,user_id,hidden) KEY(conversation_id,user_id) VALUES(?,?,FALSE)", conversation, user);
    }

    private void connect(long a, long b) {
        jdbc.update("MERGE INTO accepted_connection(user_low_id,user_high_id) KEY(user_low_id,user_high_id) VALUES(?,?)", Math.min(a,b), Math.max(a,b));
    }
}
