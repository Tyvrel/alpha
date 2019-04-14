package application.slide.domain;

import application.slide.infrastructure.SlideRepository;
import io.vavr.collection.List;
import io.vavr.collection.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;


@Slf4j
@RequiredArgsConstructor
public class SlideService {

    private final SlideRepository slideRepository;
    private final SentenceSplittingServiceFactory sentenceSplittingServiceFactory;
    private final ExecutorService executorService;

    public boolean slideExists(String inputSentence) {
        Slide sentenceSlide = new Slide(inputSentence.split(" "));
        int sentenceSize = sentenceSlide.size();
        return !Stream.iterate(sentenceSize, slideSize -> slideSize - 1)
                .take(sentenceSize)
                .filter(slideSize -> slideExists(sentenceSlide, slideSize))
                .isEmpty();
    }

    private boolean slideExists(Slide sentence, int slideSize) {
        return !Stream.iterate(0, slideFromIndex -> slideFromIndex + 1)
                .take(sentence.size() - slideSize + 1)
                .map(slideFromIndex -> new Range(slideFromIndex, slideFromIndex + slideSize))
                .map(sentence::buildSubSlide)
                .filter(slideRepository::exists)
                .isEmpty();
    }

    public List<RandomSlide> findSlides(String inputSentence) {
        if (inputSentence == null || inputSentence.isBlank() || inputSentence.contains("  ")) {
            throw new IllegalArgumentException(String.format("Sentence %s is invalid", inputSentence));
        }

        Slide sentenceSlide = new Slide(inputSentence.split(" "));
        int sentenceSize = sentenceSlide.size();

        List<Slide> baseSentenceSlides = List.of(sentenceSlide);
        SplittingResult baseSplittingResult = new SplittingResult(List.empty(), baseSentenceSlides);
        SplittingResult reducedSplittingResult = findSplittingResult(sentenceSize, baseSplittingResult);
        return reducedSplittingResult.getSplittingSlides();
    }

    private SplittingResult findSplittingResult(int sentenceSize, SplittingResult baseSplittingResult) {
        @RequiredArgsConstructor
        class Tuple {
            final List<RandomSlide> slides;
            final int slideSize;
        }

        return Stream.iterate(sentenceSize, slideSize -> slideSize - 1)
                .take(sentenceSize)
                .peek(slideSize -> log.info("Slide size: " + slideSize))
                .map(slideSize -> new Tuple(slideRepository.findAllBySize(slideSize), slideSize))
                .filter(tuple -> !tuple.slides.isEmpty())
                .peek(tuple -> log.info("Analyzed slides: " + tuple.slides))
                .map(tuple -> sentenceSplittingServiceFactory.build(tuple.slides, tuple.slideSize, executorService))
                .foldLeft(baseSplittingResult, (previousSplittingResult, sentenceSplittingService) ->
                        foldSplittingResultDroppingOldSentences(previousSplittingResult, splitSentencesBySlides(previousSplittingResult.getSentences(), sentenceSplittingService)));
    }

    private SplittingResult splitSentencesBySlides(List<Slide> sentences, SentenceSplittingService sentenceSplittingService) {
        return sentences
                .map(sentenceSplittingService::splitSentenceBySlidesAsync)
                .map(this::toBlocking)
                .foldLeft(new SplittingResult(), this::foldSplittingResult);
    }

    private SplittingResult toBlocking(CompletableFuture<SplittingResult> splittingResultCompletableFuture) {
        try {
            return splittingResultCompletableFuture.get();
        } catch (InterruptedException | ExecutionException e) {
            throw new SentenceSplittingException(e.getMessage(), e);
        }
    }

    private SplittingResult foldSplittingResultDroppingOldSentences(SplittingResult splittingResult1, SplittingResult splittingResult2) {
        return new SplittingResult(
                splittingResult1.getSplittingSlides().appendAll(splittingResult2.getSplittingSlides()),
                splittingResult2.getSentences()
        );
    }

    private SplittingResult foldSplittingResult(SplittingResult splittingResult1, SplittingResult splittingResult2) {
        return new SplittingResult(
                splittingResult1.getSplittingSlides().appendAll(splittingResult2.getSplittingSlides()),
                splittingResult1.getSentences().appendAll(splittingResult2.getSentences())
        );
    }
}
