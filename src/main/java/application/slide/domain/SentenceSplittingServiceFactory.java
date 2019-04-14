package application.slide.domain;

import io.vavr.collection.List;

import java.util.concurrent.ExecutorService;

class SentenceSplittingServiceFactory {
    SentenceSplittingService build(List<RandomSlide> slides, int slideSize, ExecutorService executorService) {
        return new SentenceSplittingService(slides, slideSize, executorService);
    }
}
