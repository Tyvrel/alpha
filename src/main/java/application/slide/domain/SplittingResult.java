package application.slide.domain;

import io.vavr.collection.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;


@Value
@RequiredArgsConstructor
class SplittingResult {
    private final List<RandomSlide> splittingSlides;
    private final List<Slide> sentences;

    SplittingResult() {
        this.splittingSlides = List.empty();
        this.sentences = List.empty();
    }
}
