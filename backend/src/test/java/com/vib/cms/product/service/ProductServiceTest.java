package com.vib.cms.product.service;

import com.vib.cms.common.exception.ResourceNotFoundException;
import com.vib.cms.product.api.PageResponse;
import com.vib.cms.product.api.ProductRequest;
import com.vib.cms.product.api.ProductResponse;
import com.vib.cms.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository repository;

    private ProductService service;

    @BeforeEach
    void setUp() {
        service = new ProductService(repository);
    }

    @Test
    void listRejectsNegativePage() {
        assertThrows(IllegalArgumentException.class,
                () -> service.list(-1, 20, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void listRejectsSizeAboveMaximum() {
        assertThrows(IllegalArgumentException.class,
                () -> service.list(0, 101, null, null));
        verifyNoInteractions(repository);
    }

    @Test
    void listNormalizesStatus() {
        PageResponse<ProductResponse> expected =
                new PageResponse<>(List.of(), 0, 20, 0, 0);
        when(repository.findPage(0, 20, "loan", "ACTIVE")).thenReturn(expected);

        assertEquals(expected, service.list(0, 20, "loan", " active "));
    }

    @Test
    void listRejectsUnknownStatus() {
        assertThrows(IllegalArgumentException.class,
                () -> service.list(0, 20, null, "DELETED"));
        verifyNoInteractions(repository);
    }

    @Test
    void createDelegatesToRepository() {
        ProductRequest request = new ProductRequest("PWS", "Public Website", "desc");
        ProductResponse expected = product("ACTIVE");
        when(repository.insert(request, "tester")).thenReturn(expected);

        assertEquals(expected, service.create(request, "tester"));
        verify(repository).insert(request, "tester");
    }

    @Test
    void updateRejectsArchivedProduct() {
        UUID id = UUID.randomUUID();
        when(repository.find(id)).thenReturn(product(id, "ARCHIVED"));

        assertThrows(IllegalArgumentException.class,
                () -> service.update(id,
                        new ProductRequest("PWS", "Public Website", null), "tester"));
        verify(repository, never()).update(any(), any(), any());
    }

    @Test
    void changeStatusDoesNotAllowArchivingThroughPatch() {
        UUID id = UUID.randomUUID();
        when(repository.find(id)).thenReturn(product(id, "ACTIVE"));

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(id, "ARCHIVED", "tester"));
        verify(repository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void changeStatusRejectsArchivedProduct() {
        UUID id = UUID.randomUUID();
        when(repository.find(id)).thenReturn(product(id, "ARCHIVED"));

        assertThrows(IllegalArgumentException.class,
                () -> service.changeStatus(id, "ACTIVE", "tester"));
        verify(repository, never()).updateStatus(any(), any(), any());
    }

    @Test
    void changeStatusUpdatesActiveProduct() {
        UUID id = UUID.randomUUID();
        ProductResponse current = product(id, "ACTIVE");
        ProductResponse updated = product(id, "INACTIVE");
        when(repository.find(id)).thenReturn(current);
        when(repository.updateStatus(id, "INACTIVE", "tester")).thenReturn(updated);

        assertEquals(updated, service.changeStatus(id, "inactive", "tester"));
    }

    private ProductResponse product(String status) {
        return product(UUID.randomUUID(), status);
    }

    private ProductResponse product(UUID id, String status) {
        OffsetDateTime now = OffsetDateTime.now();
        return new ProductResponse(id, "PWS", "Public Website", "desc",
                status, now, "tester", now, "tester");
    }
}
