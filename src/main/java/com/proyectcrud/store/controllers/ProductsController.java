package com.proyectcrud.store.controllers;

import com.proyectcrud.store.models.Product;
import com.proyectcrud.store.models.ProductDto;
import com.proyectcrud.store.services.ProductsRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.validation.Valid;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Date;
import java.util.Optional;

@Controller
@RequestMapping("/products")
public class ProductsController {

    @Autowired
    private ProductsRepository repo;

    @GetMapping({"", "/"})
    public String showProductList(Model model) {
        model.addAttribute("products", repo.findAll(Sort.by(Sort.Direction.DESC, "id")));
        return "products/index";
    }

    @GetMapping("/create")
    public String showCreatePage(Model model) {
        ProductDto productDto = new ProductDto();
        model.addAttribute("productDto", productDto);
        return "products/CreateProduct";
    }

    @PostMapping("/create")
    public String createProduct(
            @Valid @ModelAttribute ProductDto productDto,
            BindingResult result
    ) {
        if (productDto.getImageFile().isEmpty()) {
            result.addError(new FieldError("productDto", "imageFile", "Se requiere el archivo de imagen"));
        }

        if (result.hasErrors()) {
            return "products/CreateProduct";
        }

        // Guardar archivo de imagen
        MultipartFile image = productDto.getImageFile();
        Date createAt = new Date();
        String storageFileName = createAt.getTime() + "_" + image.getOriginalFilename();

        try {
            String uploadDir = "public/images/";
            Path uploadPath = Paths.get(uploadDir);

            if (!Files.exists(uploadPath)) {
                Files.createDirectories(uploadPath);
            }

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, uploadPath.resolve(storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        Product product = new Product();
        product.setNombre(productDto.getNombre());
        product.setMarca(productDto.getMarca());
        product.setCategoria(productDto.getCategoria());
        product.setPrecio(productDto.getPrecio());
        product.setDescripcion(productDto.getDescription());
        product.setCreateAt(createAt);
        product.setNombreImagen(storageFileName);

        repo.save(product);

        return "redirect:/products";
    }

    @GetMapping("/edit")
    public String showEditPage(
            Model model,
            @RequestParam(name = "id", required = false) Long id
    ) {
        if (id == null) {
            return "redirect:/products";
        }

        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) {
            return "redirect:/products";
        }

        Product product = optionalProduct.get();
        model.addAttribute("product", product);

        ProductDto productDto = new ProductDto();
        productDto.setId(product.getId()); // Asegúrate de que el id esté en el DTO
        productDto.setNombre(product.getNombre());
        productDto.setMarca(product.getMarca());
        productDto.setCategoria(product.getCategoria());
        productDto.setPrecio(product.getPrecio());
        productDto.setDescription(product.getDescripcion());

        model.addAttribute("productDto", productDto);

        return "products/EditProduct";
    }

    @PostMapping("/edit")
    public String updateProduct(
            @RequestParam Long id, // Asegúrate de que 'id' se envía como un parámetro
            @Valid @ModelAttribute ProductDto productDto, // DTO debe coincidir con el formulario
            BindingResult result
    ) throws IOException {
        if (result.hasErrors()) {
            return "products/EditProduct";
        }

        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) {
            return "redirect:/products";
        }

        Product product = optionalProduct.get();

        // Sí hay una imagen nueva
        if (!productDto.getImageFile().isEmpty()) {
            // Eliminar la imagen antigua
            String uploadDir = "public/images/";
            Path oldImagePath = Paths.get(uploadDir + product.getNombreImagen());

            try {
                Files.deleteIfExists(oldImagePath);
            } catch (Exception ex) {
                System.out.println("Exception: " + ex.getMessage());
            }

            // Guardar la nueva imagen
            MultipartFile image = productDto.getImageFile();
            String storageFileName = new Date().getTime() + "_" + image.getOriginalFilename();

            try (InputStream inputStream = image.getInputStream()) {
                Files.copy(inputStream, Paths.get(uploadDir + storageFileName), StandardCopyOption.REPLACE_EXISTING);
            }
            product.setNombreImagen(storageFileName);
        }

        // Actualizar otros campos del producto
        product.setNombre(productDto.getNombre());
        product.setMarca(productDto.getMarca());
        product.setCategoria(productDto.getCategoria());
        product.setPrecio(productDto.getPrecio());
        product.setDescripcion(productDto.getDescription());

        repo.save(product);

        return "redirect:/products";
    }


    @GetMapping("/delete")
    public String deleteProduct(
            @RequestParam Long id
    ) {
        if (id == null) {
            return "redirect:/products"; // Validación para id nulo
        }

        Optional<Product> optionalProduct = repo.findById(Math.toIntExact(id));
        if (optionalProduct.isEmpty()) {
            return "redirect:/products"; // Si el producto no existe, redirigir
        }

        Product product = optionalProduct.get();

        // Eliminar imagen del producto
        Path imagePath = Paths.get("public/images/" + product.getNombreImagen());

        try {
            Files.deleteIfExists(imagePath); // Cambiado a deleteIfExists para evitar excepciones
        } catch (Exception ex) {
            System.out.println("Exception: " + ex.getMessage());
        }

        // Borrar el producto
        repo.delete(product);

        return "redirect:/products";
    }
}




