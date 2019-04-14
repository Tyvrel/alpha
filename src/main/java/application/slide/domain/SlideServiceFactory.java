package application.slide.domain;

import application.slide.infrastructure.SlideRepository;

import java.util.concurrent.ExecutorService;

public class SlideServiceFactory {
    public SlideService build(ExecutorService executorService, SlideRepository slideRepository) {
        SentenceSplittingServiceFactory sentenceSplittingServiceFactory = new SentenceSplittingServiceFactory();
        return new SlideService(slideRepository, sentenceSplittingServiceFactory, executorService);
    }
}
