package com.ucr.reco.service;

import com.ucr.reco.model.Space;
import com.ucr.reco.repository.SpaceJpaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SpaceServiceTest {

    //Dependencia simulada
    @Mock
    private SpaceJpaRepository repository;

    //Clase que se prueba con el mock inyectado
    @InjectMocks
    private SpaceService service;

    //Datos reutilizables en todos los tests
    private Space space;

    @BeforeEach
    void setup() {
        //Antes del @Test, se ejecuta para inicializar datos
        space = new Space(1, "Sala de Audio", "Edificio A", "Sala", 15000.00);
    }

    //Test 1: findAll() retorna la lista completa de espacios
    @Test
    void shouldReturnAllSpaces() {
        // ARRANGE: se crea un segundo espacio y se simula que el repositorio devuelve una lista con ambos espacios
        Space space2 = new Space(2, "Sala de Video", "Edificio B", "Sala", 20000.00);
        when(repository.findAll()).thenReturn(Arrays.asList(space, space2));

        // ACT: se invoca el servicio que internamente llama a repository.findAll()
        List<Space> resultado = service.findAll();

        // ASSERT: la lista no debe ser nula y debe contener exactamente 2 elementos
        assertNotNull(resultado);
        assertEquals(2, resultado.size());

        // VERIFY: findAll() del repositorio fue llamado exactamente 1 vez
        verify(repository, times(1)).findAll();
    }

    //Test 2: add() exitoso: espacio nuevo con nombre único y datos completos
    @Test
    void shouldAddSpacesWhenNotExist() {
        // ARRANGE: getByName() no encuentra el nombre (null) y save() devuelve el espacio persistido
        when(repository.getByName("Sala de Audio")).thenReturn(null);
        when(repository.save(space)).thenReturn(space);

        // ACT: se invoca add() del servicio
        Space resultado = service.add(space);

        // ASSERT: el espaDcio guardado no es nulo, su nombre y precio coinciden
        assertNotNull(resultado);
        assertEquals("Sala de Audio", resultado.getName());
        assertEquals(15000.0, resultado.getPrice());

        // VERIFY: getByName() verificó unicidad 1 vez, save() persistió 1 vez
        verify(repository, times(1)).getByName("Sala de Audio");
        verify(repository, times(1)).save(space);
    }

    //Test 3: add() falla: el espacio ya existe → retorna null
    @Test
    void shouldReturnNullWhenSpaceAlreadyExists() {
        // ARRANGE: getByName() encuentra un espacio con ese nombre, simulando duplicado
        when(repository.getByName("Sala de Audio")).thenReturn(space);

        // ACT: se intenta agregar el espacio
        Space resultado = service.add(space);

        // ASSERT: retorna null porque el nombre ya está registrado
        assertNull(resultado);

        // VERIFY: save() nunca debe ejecutarse si el espacio ya existe
        verify(repository, never()).save(any());
    }

    //Test 4: add() falla: datos incompletos (campo price = null)
    @Test
    void shouldReturnNullWhenDataIsIncomplete() {
        // ARRANGE: espacio sin precio (price = null) para probar validación de campos
        Space spaceIncompleto = new Space(3, "Sala de Arte", "Edificio C", "Sala", null);
        when(repository.getByName("Sala de Arte")).thenReturn(null);

        // ACT: se intenta agregar el espacio con datos incompletos
        Space resultado = service.add(spaceIncompleto);

        // ASSERT: retorna null porque hay campos obligatorios nulos
        assertNull(resultado);

        // VERIFY: save() nunca debe ejecutarse si los datos no son válidos
        verify(repository, never()).save(any());
    }

    //Test 5: getById() exitoso: retorna el espacio cuando existe
    @Test
    void shouldReturnSpaceById() {
        // ARRANGE: getById() retorna el espacio predefinido con id = 1
        when(repository.getById(1)).thenReturn(space);

        // ACT: se solicita el espacio por su id
        Space resultado = service.getById(1);

        // ASSERT: el resultado no es nulo, el id y el nombre coinciden
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());
        assertEquals("Sala de Audio", resultado.getName());

        // VERIFY: getById() del repositorio se invocó exactamente 1 vez
        verify(repository, times(1)).getById(1);
    }

    //Test 6: getById() falla: espacio no existe → retorna null
    @Test
    void shouldReturnNullWhenGetByIdNotFound() {
        // ARRANGE: getById() retorna null para un id que no existe
        when(repository.getById(99)).thenReturn(null);

        // ACT: se solicita un espacio con id inexistente
        Space resultado = service.getById(99);

        // ASSERT: el servicio retorna null
        assertNull(resultado);

        // VERIFY: getById() se llamó peSro no encontró el espacio
        verify(repository, times(1)).getById(99);
    }

    //Test 7: update() exitoso: actualiza solo los campos no nulos
    @Test
    void shouldUpdateSpaceWhenExists() {
        // ARRANGE: espacio con solo name y price (location y type null para probar que solo sobrescribe campos no nulos)
        Space spaceActualizado = new Space(1, "Sala de Audio Renovada", null, null, 18000.0);
        when(repository.getById(1)).thenReturn(space);
        when(repository.save(space)).thenReturn(space);

        // ACT: se invoca update() con los campos a modificar
        Space resultado = service.update(spaceActualizado);

        // ASSERT: el nombre y precio se actualizaron, location y type conservan valores originales
        assertNotNull(resultado);
        assertEquals("Sala de Audio Renovada", resultado.getName());
        assertEquals(18000.0, resultado.getPrice());

        // VERIFY: getById() buscó el espacio existente y save() persistió los cambios
        verify(repository, times(1)).getById(1);
        verify(repository, times(1)).save(space);
    }

    //Test 8: update() falla: espacio no existe → retorna null
    @Test
    void shouldReturnNullWhenUpdateSpaceNotExists() {
        // ARRANGE: getById() no encuentra el espacio con id = 99
        when(repository.getById(99)).thenReturn(null);

        Space spaceInexistente = new Space(99, "Fantasma", "Ninguno", "N/A", 0.0);

        // ACT: se intenta actualizar un espacio que no existe
        Space resultado = service.update(spaceInexistente);

        // ASSERT: el servicio retorna null
        assertNull(resultado);

        // VERIFY: save() nunca debe ser llamado si el espacio no existe
        verify(repository, never()).save(any());
    }

    //Test 9: delete() exitoso: elimina el espacio y lo retorna
    @Test
    void shouldDeleteSpaceWhenExists() {
        // ARRANGE: getById() encuentra el espacio y deleteById() se simula sin excepción
        when(repository.getById(1)).thenReturn(space);
        doNothing().when(repository).deleteById(1);

        // ACT: se elimina el espacio por su id
        Space resultado = service.delete(1);

        // ASSERT: el espacio eliminado no es nulo y su id coincide
        assertNotNull(resultado);
        assertEquals(1, resultado.getId());

        // VERIFY: getById() verificó existencia y deleteById() ejecutó la eliminación
        verify(repository, times(1)).getById(1);
        verify(repository, times(1)).deleteById(1);
    }

    //Test 10: delete() falla: espacio no existe → retorna null
    @Test
    void shouldReturnNullWhenDeleteSpaceNotExists() {
        // ARRANGE: getById() retorna null porque el id 99 no existe
        when(repository.getById(99)).thenReturn(null);

        // ACT: se intenta eliminar un espacio inexistente
        Space resultado = service.delete(99);

        // ASSERT: el servicio retorna null
        assertNull(resultado);

        // VERIFY: deleteById() nunca debe ejecutarse si el espacio no existe
        verify(repository, never()).deleteById(any());
    }

    //Test 11: changePrice() exitoso: actualiza el precio del espacio
    @Test
    void shouldChangePriceWhenSpaceExists() {
        // ARRANGE: getById() encuentra el espacio y save() persiste el cambio
        when(repository.getById(1)).thenReturn(space);
        when(repository.save(space)).thenReturn(space);

        // ACT: se cambia el precio del espacio con id = 1 a 25000.0
        Space resultado = service.changePrice(1, 25000.0);

        // ASSERT: el precio del espacio debe ser el nuevo valor asignado
        assertNotNull(resultado);
        assertEquals(25000.0, resultado.getPrice());

        // VERIFY: getById() buscó el espacio y save() guardó el precio actualizado
        verify(repository, times(1)).getById(1);
        verify(repository, times(1)).save(space);
    }

    //Test 12: changePrice() falla: espacio no existe → retorna null
    @Test
    void shouldReturnNullWhenChangePriceSpaceNotExists() {
        // ARRANGE: getById() retorna null porque el id 99 no existe
        when(repository.getById(99)).thenReturn(null);

        // ACT: se intenta cambiar el precio de un espacio inexistente
        Space resultado = service.changePrice(99, 25000.0);

        // ASSERT: el servicio retorna null
        assertNull(resultado);

        // VERIFY: save() nunca debe ejecutarse si el espacio no existe
        verify(repository, never()).save(any());
    }


}