package application.slide.infrastructure;

import application.slide.domain.SlideService;
import application.slide.domain.SlideServiceFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Random;
import java.util.concurrent.ExecutorService;

@Configuration
public class SlideConfiguration {
    @Bean
    public SlideService slideService(ExecutorService executorService, Random random) {
        SlideRepository slideRepository = new SlideRepository(random);
        return new SlideServiceFactory().build(executorService, slideRepository);
    }
}
