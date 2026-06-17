package com.research.backend.config;

import com.research.backend.entity.User;
import com.research.backend.neo4j.entity.Keyword;
import com.research.backend.neo4j.entity.Reviewer;
import com.research.backend.neo4j.repository.KeywordRepository;
import com.research.backend.neo4j.repository.ReviewerRepository;
import com.research.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final ReviewerRepository reviewerRepository;
    private final KeywordRepository keywordRepository;

    private static final List<String> TECH_KEYWORDS = Arrays.asList(
            "深度学习", "机器学习", "计算机视觉", "自然语言处理", "知识图谱",
            "强化学习", "图神经网络", "Transformer", "生成模型", "数据挖掘"
    );

    @Override
    public void run(String... args) {
        log.info("开始初始化数据...");
        initUsers();
        initKeywords();
        initReviewers();
        log.info("数据初始化完成.");
    }

    private void initUsers() {
        createUserIfNotExists("admin", "admin123", "admin@review.com",
                User.UserRole.ROLE_ADMIN, "系统管理员", "系统管理机构");
        createUserIfNotExists("author1", "author123", "author1@test.com",
                User.UserRole.ROLE_AUTHOR, "张小明", "清华大学计算机系");
        createUserIfNotExists("author2", "author123", "author2@test.com",
                User.UserRole.ROLE_AUTHOR, "李华", "北京大学人工智能研究院");

        String[][] reviewerData = {
                {"reviewer1", "王教授", "中国科学院自动化所", "reviewer1@test.com"},
                {"reviewer2", "赵研究员", "浙江大学CAD&CG国家重点实验室", "reviewer2@test.com"},
                {"reviewer3", "陈博士", "上海交通大学电子信息学院", "reviewer3@test.com"},
                {"reviewer4", "刘教授", "南京大学计算机系", "reviewer4@test.com"},
                {"reviewer5", "孙研究员", "复旦大学数据科学学院", "reviewer5@test.com"}
        };
        for (String[] data : reviewerData) {
            createUserIfNotExists(data[0], "reviewer123", data[3],
                    User.UserRole.ROLE_REVIEWER, data[1], data[2]);
        }
    }

    private void createUserIfNotExists(String username, String password, String email,
                                       User.UserRole role, String name, String affiliation) {
        if (!userRepository.existsByUsername(username)) {
            User user = User.builder()
                    .username(username)
                    .password(passwordEncoder.encode(password))
                    .email(email)
                    .role(role)
                    .name(name)
                    .affiliation(affiliation)
                    .enabled(true)
                    .build();
            userRepository.save(user);
            log.info("创建用户: {}", username);
        }
    }

    private void initKeywords() {
        for (String keywordName : TECH_KEYWORDS) {
            if (!keywordRepository.existsByName(keywordName)) {
                Keyword keyword = new Keyword();
                keyword.setName(keywordName);
                keywordRepository.save(keyword);
                log.info("创建关键词节点: {}", keywordName);
            }
        }
    }

    private void initReviewers() {
        String[][] reviewerData = {
                {"reviewer1", "王教授", "中国科学院自动化所", "reviewer1@test.com", "深度学习,计算机视觉,图神经网络", "0,2,6"},
                {"reviewer2", "赵研究员", "浙江大学CAD&CG国家重点实验室", "reviewer2@test.com", "计算机视觉,深度学习,生成模型", "2,0,8"},
                {"reviewer3", "陈博士", "上海交通大学电子信息学院", "reviewer3@test.com", "自然语言处理,Transformer,机器学习", "3,7,1"},
                {"reviewer4", "刘教授", "南京大学计算机系", "reviewer4@test.com", "知识图谱,数据挖掘,图神经网络", "4,9,6"},
                {"reviewer5", "孙研究员", "复旦大学数据科学学院", "reviewer5@test.com", "强化学习,机器学习,深度学习,数据挖掘", "5,1,0,9"}
        };

        List<Keyword> allKeywords = keywordRepository.findAll();

        for (String[] data : reviewerData) {
            String username = data[0];
            String name = data[1];
            String affiliation = data[2];
            String email = data[3];
            String researchAreas = data[4];
            String[] keywordIndices = data[5].split(",");

            if (reviewerRepository.findByEmail(email).isEmpty()) {
                Reviewer reviewer = new Reviewer();
                reviewer.setName(name);
                reviewer.setAffiliation(affiliation);
                reviewer.setEmail(email);
                reviewer.setTitle("教授");
                reviewer.setResearchAreas(researchAreas);

                List<Keyword> expertKeywords = new ArrayList<>();
                for (String idxStr : keywordIndices) {
                    int idx = Integer.parseInt(idxStr.trim());
                    if (idx >= 0 && idx < allKeywords.size()) {
                        expertKeywords.add(allKeywords.get(idx));
                    }
                }
                reviewer.setExpertKeywords(expertKeywords);

                reviewerRepository.save(reviewer);
                log.info("创建评审专家节点: {}, 研究领域: {}", name, researchAreas);
            }
        }
    }
}
