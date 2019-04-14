package application.slide.domain;

import io.vavr.collection.List;
import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class Slide {
    private final List<String> words;

    public Slide(String... words) {
        this(List.of(words));
    }

    Slide buildSubSlide(Range subSlideRange) {
        return new Slide(words.slice(subSlideRange.getFrom(), subSlideRange.getTo()));
    }

    public int size() {
        return words.size();
    }
}
