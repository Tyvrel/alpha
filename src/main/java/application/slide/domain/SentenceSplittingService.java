package application.slide.domain;

import io.vavr.collection.List;
import io.vavr.collection.Stream;
import io.vavr.control.Option;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;

@Slf4j
@RequiredArgsConstructor
class SentenceSplittingService {
    private final List<RandomSlide> slides;
    private final int slideSize;
    private final ExecutorService executorService;

    CompletableFuture<SplittingResult> splitSentenceBySlidesAsync(Slide sentence) {
        log.info("Splitting sentence: " + sentence);

        return Stream.iterate(0, slideFromIndex -> slideFromIndex + 1)
                .take(sentence.size() - slideSize + 1)
                .map(slideFromIndex -> new Range(slideFromIndex, slideFromIndex + slideSize))
                .map(slideRange -> findSplittingResultAsync(slideRange, sentence))
                .flatMap(Option::toStream)
                .headOption()
                .getOrElse(() -> {
                    log.info("Slides not found");
                    return CompletableFuture.completedFuture(new SplittingResult(List.empty(), List.of(sentence)));
                });
    }

    private Option<CompletableFuture<SplittingResult>> findSplittingResultAsync(Range slideRange, Slide sentence) {
        Slide sentenceSlide = sentence.buildSubSlide(slideRange);

        return findSplittingResult(sentenceSlide)
                .map(foundSplittingResult -> {
                    Stream<CompletableFuture<SplittingResult>> subSentences = splitIntoSubSentences(sentence, slideRange)
                            .map(subSentence -> CompletableFuture.supplyAsync(() -> splitSentenceBySlidesAsync(subSentence), executorService)
                                    .thenCompose(splittingResultCompletableFuture -> splittingResultCompletableFuture)
                            );
                    return CompletableFuture.allOf(subSentences
                            .toJavaArray(CompletableFuture[]::new))
                            .thenApply(v -> subSentences.map(CompletableFuture::join))
                            .thenApply(splittingResults -> splittingResults.foldLeft(foundSplittingResult, this::foldSplittingResult));
                });
    }


    private Stream<Slide> splitIntoSubSentences(Slide sentence, Range sentenceRange) {
        return Stream.of(
                new Range(0, sentenceRange.getFrom()),
                new Range(sentenceRange.getTo(), sentence.size())
        )
                .map(sentence::buildSubSlide)
                .filter(subSentence -> subSentence.size() != 0);
    }

    private Option<SplittingResult> findSplittingResult(Slide sentenceSlide) {
        return slides.find(slide -> Objects.equals(slide.getSlide(), sentenceSlide))
                .peek(foundSlide -> log.info("Found application.slide " + foundSlide))
                .map(foundSlide -> new SplittingResult(List.of(foundSlide), List.empty()));
    }

    private SplittingResult foldSplittingResult(SplittingResult splittingResult1, SplittingResult splittingResult2) {
        return new SplittingResult(
                splittingResult1.getSplittingSlides().appendAll(splittingResult2.getSplittingSlides()),
                splittingResult1.getSentences().appendAll(splittingResult2.getSentences())
        );
    }

}
