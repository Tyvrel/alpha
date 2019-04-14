package application.slide.domain;

import lombok.RequiredArgsConstructor;
import lombok.Value;

@Value
@RequiredArgsConstructor
public class RandomSlide {
    private final Slide slide;
    private final int random;
}
