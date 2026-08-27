package ru.practicum.yakovlev.api;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import ru.practicum.yakovlev.dto.CreatePostRequest;
import ru.practicum.yakovlev.dto.FullPostResponseDto;
import ru.practicum.yakovlev.dto.PageResponse;
import ru.practicum.yakovlev.dto.UpdatePostRequest;

@RestController
@RequestMapping(ApiConstants.POSTS_BASE_PATH)
public interface PostController {

    @GetMapping
    PageResponse getPosts(@RequestParam("search") String searchRequest,
                          @RequestParam("pageNumber")
                          @Min(value = ApiConstants.MIN_PAGE_NUMBER, message = "Page number must be greater than zero") Integer page,
                          @RequestParam("pageSize")
                          @Min(value = ApiConstants.MIN_PAGE_SIZE, message = "Page size must be greater than zero")
                          @Max(value = ApiConstants.MAX_PAGE_SIZE, message = "Page size must not exceed {value}") Integer size);

    @PostMapping
    FullPostResponseDto createPost(@RequestBody @Valid CreatePostRequest postRequest);

    @PutMapping("/{id}")
    FullPostResponseDto updatePost(@PathVariable("id") @Positive Long id,
                                   @RequestBody @Valid UpdatePostRequest postRequest);

    @GetMapping("/{id}")
    FullPostResponseDto getPost(@PathVariable("id") @Positive Long id);

    @DeleteMapping("/{id}")
    void deletePost(@PathVariable("id") @Positive Long id);

    @PostMapping("/{id}/likes")
    Long likePost(@PathVariable("id") @Positive Long id);

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    void postImage(@PathVariable("id") @Positive Long id, @RequestParam("image") MultipartFile image);

    @GetMapping("/{id}/image")
    byte[] getPostImage(@PathVariable("id") @Positive Long id);

}
