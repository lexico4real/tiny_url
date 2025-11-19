package com.oluyinkabright.tiny_url.services;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class CodeGeneratorTest {

    private final CodeGenerator codeGenerator = new CodeGenerator();

    @Test
    void generateCode_shouldReturnCodeWithDefaultLength() {
        String code = codeGenerator.generateCode();
        assertEquals(6, code.length());
    }

    @Test
    void generateCode_shouldReturnCodeWithSpecifiedLength() {
        String code = codeGenerator.generateCode(8);
        assertEquals(8, code.length());
    }

    @Test
    void generateCode_shouldContainOnlyBase62Characters() {
        String code = codeGenerator.generateCode();
        assertTrue(code.matches("^[0-9A-Za-z]+$"));
    }

    @Test
    void generateUniqueCode_shouldReturnUniqueCodeWhenNotExists() {
        String code = codeGenerator.generateUniqueCode(c -> false);
        assertNotNull(code);
        assertEquals(6, code.length());
    }

    @Test
    void generateUniqueCode_shouldRetryOnCollision() {
        final int[] calls = {0};
        String code = codeGenerator.generateUniqueCode(c -> {
            calls[0]++;
            return calls[0] <= 2; // First two calls return true (collision), third returns false
        }, 6, 5);

        assertNotNull(code);
        assertEquals(3, calls[0]);
    }

    @Test
    void generateUniqueCode_shouldThrowAfterMaxRetries() {
        assertThrows(IllegalStateException.class, () ->
                codeGenerator.generateUniqueCode(c -> true, 6, 3)
        );
    }

    @Test
    void generateCode_shouldProduceDifferentCodes() {
        String code1 = codeGenerator.generateCode();
        String code2 = codeGenerator.generateCode();

        // While there's a small chance of collision, it's very unlikely with proper randomness
        assertNotEquals(code1, code2);
    }
}
