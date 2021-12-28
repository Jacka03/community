package com.community;


import com.community.service.ReadingService;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;

@RunWith(SpringRunner.class)
@SpringBootTest
@ContextConfiguration(classes = CommunityApplication.class)
public class ReadingTest {
    private static final Logger logger = LoggerFactory.getLogger(ReadingTest.class);

    @Autowired
    private ReadingService readingService;

    @Test
    public void readingTest() {
        readingService.reading(109);
    }
}
