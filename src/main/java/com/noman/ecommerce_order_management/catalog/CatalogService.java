package com.noman.ecommerce_order_management.catalog;

import com.noman.ecommerce_order_management.catalog.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CatalogService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;
    private final SkuRepository skuRepository;

    // ---------------- CATEGORY ----------------

    @Transactional
    public CategoryResponse createCategory(CategoryRequest request) {

        String categoryName = request.getName().trim();

        categoryRepository.findByNameIgnoreCase(categoryName)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Category already exists"
                    );
                });

        Category category = Category.builder()
                .name(categoryName)
                .description(trimToNull(request.getDescription()))
                .build();

        return toCategoryResponse(
                categoryRepository.save(category)
        );
    }

    @Transactional
    public CategoryResponse updateCategory(
            Long categoryId,
            CategoryRequest request
    ) {

        Category category = getCategory(categoryId);

        String categoryName = request.getName().trim();

        categoryRepository.findByNameIgnoreCase(categoryName)
                .filter(existing ->
                        !existing.getId().equals(categoryId)
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "Category already exists"
                    );
                });

        category.setName(categoryName);
        category.setDescription(
                trimToNull(request.getDescription())
        );

        return toCategoryResponse(category);
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategories() {

        return categoryRepository
                .findAllByOrderByNameAsc()
                .stream()
                .map(this::toCategoryResponse)
                .toList();
    }

    // ---------------- PRODUCT ----------------

    @Transactional
    public ProductResponse createProduct(
            ProductRequest request
    ) {

        Category category = getCategory(
                request.getCategoryId()
        );

        Product product = Product.builder()
                .name(request.getName().trim())
                .description(
                        trimToNull(request.getDescription())
                )
                .brand(request.getBrand().trim())
                .category(category)
                .active(true)
                .build();

        return toProductResponse(
                productRepository.save(product)
        );
    }

    @Transactional
    public ProductResponse updateProduct(
            Long productId,
            ProductRequest request
    ) {

        Product product = getProduct(productId);

        Category category = getCategory(
                request.getCategoryId()
        );

        product.setName(request.getName().trim());
        product.setDescription(
                trimToNull(request.getDescription())
        );
        product.setBrand(request.getBrand().trim());
        product.setCategory(category);

        return toProductResponse(product);
    }

    @Transactional
    public ProductResponse updateProductStatus(
            Long productId,
            boolean active
    ) {

        Product product = getProduct(productId);

        product.setActive(active);

        return toProductResponse(product);
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> getProducts(
            Long categoryId
    ) {

        List<Product> products;

        if (categoryId == null) {

            products =
                    productRepository
                            .findByActiveTrueOrderByNameAsc();

        } else {

            getCategory(categoryId);

            products =
                    productRepository
                            .findByCategoryIdAndActiveTrueOrderByNameAsc(
                                    categoryId
                            );
        }

        return products.stream()
                .map(this::toProductResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public ProductDetailsResponse getProductDetails(
            Long productId
    ) {

        Product product =
                productRepository
                        .findByIdAndActiveTrue(productId)
                        .orElseThrow(() ->
                                new ResponseStatusException(
                                        HttpStatus.NOT_FOUND,
                                        "Product not found"
                                )
                        );

        List<SkuResponse> skus =
                skuRepository
                        .findByProductIdAndActiveTrueOrderByPriceAsc(
                                productId
                        )
                        .stream()
                        .map(this::toSkuResponse)
                        .toList();

        return ProductDetailsResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(
                        product.getCategory().getId()
                )
                .categoryName(
                        product.getCategory().getName()
                )
                .active(product.isActive())
                .skus(skus)
                .build();
    }

    // ---------------- SKU ----------------

    @Transactional
    public SkuResponse createSku(
            Long productId,
            SkuRequest request
    ) {

        Product product = getProduct(productId);

        String skuCode =
                request.getSkuCode()
                        .trim()
                        .toUpperCase();

        skuRepository
                .findBySkuCodeIgnoreCase(skuCode)
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "SKU code already exists"
                    );
                });

        Sku sku = Sku.builder()
                .skuCode(skuCode)
                .variantName(
                        trimToNull(request.getVariantName())
                )
                .price(request.getPrice())
                .product(product)
                .active(true)
                .build();

        return toSkuResponse(
                skuRepository.save(sku)
        );
    }

    @Transactional
    public SkuResponse updateSku(
            Long skuId,
            SkuRequest request
    ) {

        Sku sku = getSku(skuId);

        String skuCode =
                request.getSkuCode()
                        .trim()
                        .toUpperCase();

        skuRepository
                .findBySkuCodeIgnoreCase(skuCode)
                .filter(existing ->
                        !existing.getId().equals(skuId)
                )
                .ifPresent(existing -> {
                    throw new ResponseStatusException(
                            HttpStatus.CONFLICT,
                            "SKU code already exists"
                    );
                });

        sku.setSkuCode(skuCode);
        sku.setVariantName(
                trimToNull(request.getVariantName())
        );
        sku.setPrice(request.getPrice());

        return toSkuResponse(sku);
    }

    @Transactional
    public SkuResponse updateSkuStatus(
            Long skuId,
            boolean active
    ) {

        Sku sku = getSku(skuId);

        sku.setActive(active);

        return toSkuResponse(sku);
    }

    // ---------------- PRIVATE HELPERS ----------------

    private Category getCategory(Long categoryId) {

        return categoryRepository
                .findById(categoryId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Category not found"
                        )
                );
    }

    private Product getProduct(Long productId) {

        return productRepository
                .findById(productId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "Product not found"
                        )
                );
    }

    private Sku getSku(Long skuId) {

        return skuRepository
                .findById(skuId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.NOT_FOUND,
                                "SKU not found"
                        )
                );
    }

    private CategoryResponse toCategoryResponse(
            Category category
    ) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .build();
    }

    private ProductResponse toProductResponse(
            Product product
    ) {

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .brand(product.getBrand())
                .categoryId(
                        product.getCategory().getId()
                )
                .categoryName(
                        product.getCategory().getName()
                )
                .active(product.isActive())
                .build();
    }

    private SkuResponse toSkuResponse(Sku sku) {

        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .variantName(sku.getVariantName())
                .price(sku.getPrice())
                .active(sku.isActive())
                .build();
    }

    private String trimToNull(String value) {

        if (value == null) {
            return null;
        }

        String trimmed = value.trim();

        return trimmed.isEmpty()
                ? null
                : trimmed;
    }
}