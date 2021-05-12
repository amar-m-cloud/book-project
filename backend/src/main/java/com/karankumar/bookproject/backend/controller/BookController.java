/*
 * The book project lets a user keep track of different books they would like to read, are currently
 * reading, have read or did not finish.
 * Copyright (C) 2021  Karan Kumar
 * This program is free software: you can redistribute it and/or modify it under the terms of the
 * GNU General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR
 * PURPOSE.  See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with this program.
 * If not, see <https://www.gnu.org/licenses/>.
 */

package com.karankumar.bookproject.backend.controller;

import org.apache.commons.lang3.NotImplementedException;
import org.modelmapper.Converter;
import org.modelmapper.AbstractConverter;
import org.modelmapper.ModelMapper;
import com.karankumar.bookproject.backend.dto.BookDto;
import com.karankumar.bookproject.backend.model.Book;
import com.karankumar.bookproject.backend.model.BookGenre;
import com.karankumar.bookproject.backend.model.BookFormat;
import com.karankumar.bookproject.backend.model.PredefinedShelf;
import com.karankumar.bookproject.backend.service.BookService;
import com.karankumar.bookproject.backend.service.PredefinedShelfService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/my-books")
public class BookController {
	
    private final BookService bookService;
    private final PredefinedShelfService predefinedShelfService;
    private final ModelMapper modelMapper;

    private static final String BOOK_NOT_FOUND_ERROR_MESSAGE = "Could not find book with ID %d";

    @Autowired
    public BookController(BookService bookService, PredefinedShelfService predefinedShelfService,
    		ModelMapper modelMapper) {
        this.bookService = bookService;
        this.predefinedShelfService = predefinedShelfService;
        this.modelMapper = modelMapper;

        this.modelMapper.addConverter(predefinedShelfConverter);
        this.modelMapper.addConverter(bookGenreConverter);
        this.modelMapper.addConverter(bookFormatConverter);
    }

    // TODO: fix this. A book always gets a null predefined shelf
    Converter<String, PredefinedShelf> predefinedShelfConverter = new AbstractConverter<>() {
        //@Override
        public PredefinedShelf convert(String predefinedShelf) {
            PredefinedShelf.ShelfName optionalPredefinedShelfName =
                    PredefinedShelf.ShelfName.valueOf(predefinedShelf);

            // Optional<PredefinedShelf> optionalPredefinedShelf = 
            //         predefinedShelfService.getPredefinedShelfByPredefinedShelfName(optionalPredefinedShelfName);
            Optional<PredefinedShelf> optionalPredefinedShelf = 
                    predefinedShelfService.getPredefinedShelfByNameAsString(predefinedShelf);

            if (optionalPredefinedShelf.isEmpty()) {
                String errorMessage = String.format(
                        "%s does not match a predefined shelf",
                        predefinedShelf
                );
                throw new IllegalStateException(errorMessage);
            }

            return optionalPredefinedShelf.get();
        }
    };

    Converter<String, BookGenre> bookGenreConverter = new AbstractConverter<>() {
        public BookGenre convert(String bookGenreString) {
            return BookGenre.valueOf(bookGenreString);
        }
    };

    Converter<String, BookFormat> bookFormatConverter = new AbstractConverter<>() {
        public BookFormat convert(String bookFormatString) {
            return BookFormat.valueOf(bookFormatString);
        }
    };

    @GetMapping()
    // TODO: only retrieve books that belong to the logged in user
    public List<Book> all() {
        return bookService.findAll();
    }
    
    @GetMapping("/find-by-id/{id}")
    // TODO: only retrieve books that belong to the logged in user
    public Book findById(@PathVariable Long id) {
    	return bookService.findById(id)
    		.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format(BOOK_NOT_FOUND_ERROR_MESSAGE, id))
            );
    }


    // TODO fix. This always returns an empty list
    // TODO: only retrieve books that belong to the logged in user
    @GetMapping("/find-by-shelf-and-title-or-author/{shelfName}/{titleOrAuthor}")
    public List<Book> findByShelfAndTitleOrAuthor(@PathVariable String shelfName,
                                                  @PathVariable String titleOrAuthor) {
        throw new NotImplementedException();
//        return bookService.findByShelfAndTitleOrAuthor(shelfName, titleOrAuthor);
//        return bookService.findByShelfAndTitleOrAuthor2(shelfName);
    }

    @GetMapping("/find-by-title-or-author/{titleOrAuthor}")
    // TODO: only retrieve books that belong to the logged in user
    public List<Book> findByTitleOrAuthor(@PathVariable String titleOrAuthor) {
        throw new NotImplementedException();
//        return bookService.findByTitleOrAuthor(titleOrAuthor);
    }

    @PostMapping()
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<Book> addBook(@RequestBody BookDto bookDto) {
    	Book bookToAdd = convertToBook(bookDto);
        return bookService.save(bookToAdd);
    }

    @PostMapping("/add-to-read-book")
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<Book> addToReadBook(@RequestBody BookDto bookDto) {
    	Book bookToAdd = convertToBook(bookDto);
    	bookToAdd.addPredefinedShelf(predefinedShelfService.findToReadShelf());
        return bookService.save(bookToAdd);
    }

    @PostMapping("/add-reading-book")
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<Book> addReadingBook(@RequestBody BookDto bookDto) {
        Book bookToAdd = convertToBook(bookDto);
        bookToAdd.addPredefinedShelf(predefinedShelfService.findReadingShelf());
        return bookService.save(bookToAdd);
    }

    @PostMapping("/add-read-book")
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<Book> addReadBook(@RequestBody BookDto bookDto) {
        Book bookToAdd = convertToBook(bookDto);
        bookToAdd.addPredefinedShelf(predefinedShelfService.findReadShelf());
        return bookService.save(bookToAdd);
    }

    @PostMapping("/add-did-not-finish-book")
    @ResponseStatus(HttpStatus.CREATED)
    public Optional<Book> addDidNotFinishBook(@RequestBody BookDto bookDto) {
        Book bookToAdd = convertToBook(bookDto);
        bookToAdd.addPredefinedShelf(predefinedShelfService.findDidNotFinishShelf());
        return bookService.save(bookToAdd);
    }

    @PatchMapping("/update-book/{id}")
    @ResponseStatus(HttpStatus.OK)
    public Optional<Book> update(@PathVariable Long id, @RequestBody Map<String, Object> changes) { //@RequestBody BookDto updatedBookDto) {
    	//fetch existing Book entity and ensure it exists
        Optional<Book> bookToUpdate = bookService.findById(id);
    	if (bookToUpdate.isEmpty()) {
    		throw new ResponseStatusException(
    		        HttpStatus.NOT_FOUND,
                    String.format(BOOK_NOT_FOUND_ERROR_MESSAGE, id)
            );
    	}

        //map persistent data to REST BookDto
        BookDto bookDtoToUpdate = convertToDto(bookToUpdate.get());

        //updatedBookDto.setId(id);
        //Book updatedBook = convertToBook(updatedBookDto);
        //modelMapper.map(updatedBookDto, bookToUpdate);
        

        //apply the changes to the REST BookDto
        // changes.forEach(
        //     (change, value) -> {
        //         switch (change){
        //             case "title": bookDtoToUpdate.setTitle((String) value); break;
        //             case "numberOfPages": bookDtoToUpdate.setNumberOfPages((Integer) value); break;
        //             case "pagesRead": bookDtoToUpdate.setPagesRead((Integer) value); break;
        //         }
        //     }
        // );
        // changes.forEach(
        //     (change, value) -> {
        //         switch (change) {
        //             case "title":
        //                 bookToUpdate.get().setTitle((String) value);
        //                 break;
        //             case "author":
        //                 bookToUpdate.get().setAuthor((Author) modelMapper.map(value, Author.class));
        //                 break;
        //             case "predefinedShelfString":
        //                 bookToUpdate.get().setPredefinedShelf((PredefinedShelf) modelMapper.map(value, PredefinedShelf.class));
        //                 break;
        //             case "numberOfPages":
        //                 bookToUpdate.get().setNumberOfPages((Integer) value);
        //                 break;
        //             case "pagesRead":
        //                 bookToUpdate.get().setPagesRead((Integer) value);
        //                 break;
        //             case "bookGenre":
        //                 bookToUpdate.get().setBookGenre((BookGenre) modelMapper.map(value, BookGenre.class));
        //                 break;
        //             case "bookFormat":
        //                 bookToUpdate.get().setBookFormat((BookFormat) modelMapper.map(value, BookFormat.class));
        //                 break;
        //             case "seriesPosition":
        //                 bookToUpdate.get().setSeriesPosition((Integer) value);
        //                 break;
        //             case "edition":
        //                 bookToUpdate.get().setEdition((Integer) value);
        //                 break;
        //             case "bookRecommendedBy":
        //                 bookToUpdate.get().setBookRecommendedBy((String) value);
        //                 break;
        //             case "isbn":
        //                 bookToUpdate.get().setIsbn((String) value);
        //             case "yearofPublication":
        //                 bookToUpdate.get().setPublicationYear((Integer) value);
        //                 break;
        //         }
        //     }
        // );

        modelMapper.map(changes, bookDtoToUpdate);
    	Book updatedBook = convertToBook(bookDtoToUpdate);

    	//updatedBookDto.setId(id);
    	//Book updatedBook = convertToBook(updatedBookDto);
        return bookService.save(updatedBook);
    	//return bookService.save(bookToUpdate.get());
    }
    
    private BookDto convertToDto(Book book) {
        return modelMapper.map(book, BookDto.class);
    }
    
    private Book convertToBook(BookDto bookDto) {
        return modelMapper.map(bookDto, Book.class);
    }

    @DeleteMapping("/delete-book/{id}")
    public void delete(@PathVariable Long id) {
    	Book bookToDelete = bookService.findById(id)
    		.orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                String.format(BOOK_NOT_FOUND_ERROR_MESSAGE, id))
        );
        bookService.delete(bookToDelete);
    }
}
