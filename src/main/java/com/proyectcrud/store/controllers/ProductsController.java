package com.proyectcrud.store.controllers;

import com.proyectcrud.store.models.Product;
import com.proyectcrud.store.models.ProductDto;
import com.proyectcrud.store.services.ProductsRepository;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.*;
import java.util.Date;
import java.util.List;
import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository repo;

    @GetMapping({"", "/"})
    public String showProductList(@RequestParam(name = "keyword", required = false) String keyword, Model model) {
        List<Product> products;

        if (keyword != null && !keyword.isEmpty()) {
            products = repo.findByMarcaContainingIgnoreCaseOrCategoriaContainingIgnoreCase(keyword, keyword);
        } else {
            products = repo.findAll(Sort.by(Sort.Direction.DESC, "id"));
        }

        model.addAttribute("products", products);
        model.addAttribute("keyword", keyword);
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        model.addAttribute("productDto", new ProductDto());
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(@Valid @ModelAttribute ProductDto productDto, BindingResult result) {
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "Se requiere el archivo de imagen"));
        }

        if (result.hasErrors()) {
            return "products/CreateProduct";
        }

        MultipartFile image = productDto.getImageFile();
        String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();
        Path uploadPath = Paths.get("public/images/");

        try {
            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }
            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        Product product = new Product();
        product.setNombre(productDto.getNombre());
        product.setMarca(productDto.getMarca());
        product.setCategoria(productDto.getCategoria());
        product.setPrecio(productDto.getPrecio());
        product.setDescripcion(productDto.getDescription());
        product.setStock(productDto.getStock());
        product.setCreateAt(new Date());
        product.setNombreImagen(storageFileName);
        product.setActivo(true);

        repo.save(product);
        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(@RequestParam(name = "id") Long id, Model model) {
        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) return "redirect:/products";

        Product product = optionalProduct.get();
        ProductDto productDto = new ProductDto();

        productDto.setId(product.getId());
        productDto.setNombre(product.getNombre());
        productDto.setMarca(product.getMarca());
        productDto.setCategoria(product.getCategoria());
        productDto.setPrecio(product.getPrecio());
        productDto.setStock(product.getStock());
        productDto.setDescription(product.getDescripcion());

        model.addAttribute("product", product);
        model.addAttribute("productDto", productDto);
        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(@RequestParam Long id, @Valid @ModelAttribute ProductDto productDto, BindingResult result) {
        if (result.hasErrors()) {
            return "products/EditProduct";
        }

        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) return "redirect:/products";

        Product product = optionalProduct.get();

        if (!productDto.getImageFile().isEmpty()) {
            Path uploadPath = Paths.get("public/images/");
            try {
                Files.deleteIfExists(uploadPath.resolve(product.getNombreImagen()));
            } catch (IOException e) {
                e.printStackTrace();
            }

            String newFileName = new Date().getTime() + "_" + productDto.getImageFile().getOriginalFilename();
            try (InputStream inputStream = productDto.getImageFile().getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(newFileName), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException e) {
                e.printStackTrace();
            }
            product.setNombreImagen(newFileName);
        }

        product.setNombre(productDto.getNombre());
        product.setMarca(productDto.getMarca());
        product.setCategoria(productDto.getCategoria());
        product.setPrecio(productDto.getPrecio());
        product.setStock(productDto.getStock());
        product.setDescripcion(productDto.getDescription());

        repo.save(product);
        return "redirect:/products";
    }

    @GetMapping("/delete")
    public String deleteProduct(@RequestParam Long id) {
        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) return "redirect:/products";

        Product product = optionalProduct.get();

        try {
            Files.deleteIfExists(Paths.get("public/images/" + product.getNombreImagen()));
        } catch (IOException e) {
            e.printStackTrace();
        }

        repo.delete(product);
        return "redirect:/products";
    }

    @GetMapping("/toggle")
    public String toggleActivo(@RequestParam Long id) {
        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isPresent()) {
            Product product = optionalProduct.get();
            product.setActivo(!product.getActivo()); // Alternar estado
            repo.save(product);
        }
        return "redirect:/products";
    }

}

