package com.dfuentes.archivo.feature.addedit

import app.cash.turbine.test
import com.dfuentes.archivo.core.model.MediaType
import com.dfuentes.archivo.core.model.Status
import com.dfuentes.archivo.fake.FakeLibraryRepository
import com.dfuentes.archivo.navigation.AddEditKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

@OptIn(ExperimentalCoroutinesApi::class)
class AddEditViewModelTest {

    private val dispatcher = StandardTestDispatcher()
    private lateinit var repository: FakeLibraryRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        repository = FakeLibraryRepository()
    }

    @After
    fun tearDown() = Dispatchers.resetMain()

    private fun viewModel(type: MediaType = MediaType.BOOK) =
        AddEditViewModel(AddEditKey(type), repository)

    @Test
    fun `un alta nueva preselecciona Terminado y la fecha de hoy`() = runTest {
        val state = viewModel().uiState.value
        assertEquals(Status.FINISHED, state.status)
        assertEquals(LocalDate.now().toEpochDay(), state.finishedOn)
    }

    @Test
    fun `no se puede guardar sin titulo`() = runTest {
        val vm = viewModel()
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddEditAction.TitleChanged("  "))
        assertFalse(vm.uiState.value.canSave)
        vm.onAction(AddEditAction.TitleChanged("Dune"))
        assertTrue(vm.uiState.value.canSave)
    }

    @Test
    fun `guardar persiste la obra con autores separados por comas`() = runTest {
        val vm = viewModel()
        vm.onAction(AddEditAction.TitleChanged("Buenos presagios"))
        vm.onAction(AddEditAction.CreatorsChanged("Terry Pratchett, Neil Gaiman"))
        vm.onAction(AddEditAction.YearChanged("1990"))
        vm.onAction(AddEditAction.SaveRequested)
        runCurrent()

        val saved = repository.works.value.single()
        assertEquals("Buenos presagios", saved.title)
        assertEquals(listOf("Terry Pratchett", "Neil Gaiman"), saved.creators)
        assertEquals(1990, saved.year)
    }

    @Test
    fun `los campos numericos ignoran lo que no sean digitos`() = runTest {
        val vm = viewModel()
        vm.onAction(AddEditAction.YearChanged("19x9y"))
        assertEquals("199", vm.uiState.value.year)
    }

    @Test
    fun `volver a pulsar la misma nota la quita`() = runTest {
        val vm = viewModel()
        vm.onAction(AddEditAction.RatingChanged(8))
        assertEquals(8, vm.uiState.value.rating)
        vm.onAction(AddEditAction.RatingChanged(8))
        assertNull(vm.uiState.value.rating)
    }

    @Test
    fun `saved pasa a true tras guardar para que la pantalla se cierre`() = runTest {
        val vm = viewModel()
        vm.onAction(AddEditAction.TitleChanged("Solaris"))
        vm.uiState.test {
            assertFalse(awaitItem().saved)
            vm.onAction(AddEditAction.SaveRequested)
            assertTrue(awaitItem().saved)
        }
    }
}
