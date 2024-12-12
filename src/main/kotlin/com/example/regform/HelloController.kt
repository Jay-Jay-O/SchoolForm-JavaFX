package com.example.regform

import javafx.application.Platform
import javafx.beans.property.SimpleStringProperty
import javafx.collections.FXCollections
import javafx.collections.ListChangeListener
import javafx.fxml.FXML
import javafx.scene.control.*
import javafx.scene.control.cell.ComboBoxTableCell
import javafx.util.Callback
import java.io.File
import javafx.beans.value.ObservableValue
import javafx.geometry.Insets
import javafx.scene.Scene
import javafx.scene.chart.PieChart
import javafx.scene.layout.StackPane
import javafx.stage.Modality
import javafx.stage.Stage
import java.nio.charset.StandardCharsets
import java.time.LocalDate
import java.io.IOException
import java.nio.file.Files
import java.nio.file.Paths
import java.nio.file.StandardOpenOption
import java.text.DecimalFormat
import java.time.format.DateTimeFormatter

data class RowData(
    val classCodeProperty: SimpleStringProperty = SimpleStringProperty(""),
    val titleProperty: SimpleStringProperty = SimpleStringProperty(""),
    val timeProperty: SimpleStringProperty = SimpleStringProperty(""),
    val dayProperty: SimpleStringProperty = SimpleStringProperty(""),
    val roomProperty: SimpleStringProperty = SimpleStringProperty(""),
    val unitCountProperty: SimpleStringProperty = SimpleStringProperty("")
) {
    var classCode: String
        get() = classCodeProperty.get()
        set(value) = classCodeProperty.set(value)

    var title: String
        get() = titleProperty.get()
        set(value) = titleProperty.set(value)

    var time: String
        get() = timeProperty.get()
        set(value) = timeProperty.set(value)

    var day: String
        get() = dayProperty.get()
        set(value) = dayProperty.set(value)

    var room: String
        get() = roomProperty.get()
        set(value) = roomProperty.set(value)

    var unitCount: String
        get() = unitCountProperty.get()
        set(value) = unitCountProperty.set(value)
}

class HelloController {
    @FXML
    private lateinit var subjectTable: TableView<RowData>
    @FXML
    private lateinit var classCodeColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var titleColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var timeColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var dayColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var roomColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var unitCountColumn: TableColumn<RowData, String>
    @FXML
    private lateinit var totalUnitsField: TextField
    @FXML
    private lateinit var semesterTF: TextField
    @FXML
    private lateinit var schoolYearTF: TextField
    @FXML
    private lateinit var datePicker: DatePicker
    @FXML
    private lateinit var idNumberTF: TextField
    @FXML
    private lateinit var courseComboBox: ComboBox<String>
    @FXML
    private lateinit var yrLevelTF: TextField
    @FXML
    private lateinit var firstNameTF: TextField
    @FXML
    private lateinit var lastNameTF: TextField
    @FXML
    private lateinit var middleNameTF: TextField
    @FXML
    private lateinit var newRbrn: RadioButton
    @FXML
    private lateinit var oldRbrn: RadioButton
    @FXML
    private lateinit var transfereeRbrn: RadioButton
    @FXML
    private lateinit var crossRbrn: RadioButton
    @FXML
    private lateinit var submitButton: Button
    @FXML
    private lateinit var showGraph: Button

    private lateinit var courseOptions: List<String>
    private lateinit var csvData: List<List<String>>
    private val toggleGroup = ToggleGroup()
    private val classCodeMap = mutableMapOf<String, List<String>>()
    private val titleMap = mutableMapOf<String, List<String>>()

    @FXML
    fun initialize() {
        csvData = loadCsvData("src/main/resources/com/example/regform/courseData.csv")

        csvData.forEach { row ->
            classCodeMap[row[0]] = row
            titleMap[row[1]] = row
        }

        setupColumn(classCodeColumn, "classCodeProperty", classCodeMap.keys.toList())
        setupColumn(titleColumn, "titleProperty", titleMap.keys.toList())
        setupOtherColumn(timeColumn, "timeProperty")
        setupOtherColumn(dayColumn, "dayProperty")
        setupOtherColumn(roomColumn, "roomProperty")
        setupOtherColumn(unitCountColumn, "unitCountProperty")

        // Correct table initialization:
        subjectTable.items = FXCollections.observableArrayList() // Initialize with empty list

        Platform.runLater { // Add initial row AFTER initialization on JavaFX thread
            subjectTable.items.add(RowData())
        }

        subjectTable.items.addListener(ListChangeListener { c ->
            while (c.next()) {
                println("List changed: ${c}") // For debugging if needed
                if (c.wasAdded() || c.wasRemoved() || c.wasUpdated()) {
                    Platform.runLater { updateUnitTotal() }
                }
            }
        })
        loadCourseOptions("src/main/resources/com/example/regform/courses.txt")
        courseComboBox.items = FXCollections.observableArrayList(courseOptions)
        datePicker.value = LocalDate.now()
        newRbrn.toggleGroup = toggleGroup
        oldRbrn.toggleGroup = toggleGroup
        transfereeRbrn.toggleGroup = toggleGroup
        crossRbrn.toggleGroup = toggleGroup

        newRbrn.isSelected = true
        submitButton.setOnAction { handleSubmission() }
        showGraph.setOnAction { showPieChart() }
    }

    private fun <T> setupColumn(column: TableColumn<RowData, T>, property: String, options: List<T>) {
        column.cellValueFactory = Callback { cellData ->
            when (property) {
                "classCodeProperty" -> cellData.value.classCodeProperty as ObservableValue<T>
                "titleProperty" -> cellData.value.titleProperty as ObservableValue<T>
                else -> null as ObservableValue<T>?
            }
        }
        column.cellFactory = ComboBoxTableCell.forTableColumn(FXCollections.observableArrayList(options))
        column.setOnEditCommit { event ->
            val row = event.rowValue
            val newValue = event.newValue
            when (property) {
                "classCodeProperty" -> row.classCode = newValue as String
                "titleProperty" -> row.title = newValue as String
            }
            updateRow(row)
            Platform.runLater {
                subjectTable.refresh()
                updateUnitTotal()
            }
        }
    }

    private fun setupOtherColumn(column: TableColumn<RowData, String>, property: String) {
        column.cellValueFactory = Callback { cellData ->
            when (property) {
                "timeProperty" -> cellData.value.timeProperty
                "dayProperty" -> cellData.value.dayProperty
                "roomProperty" -> cellData.value.roomProperty
                "unitCountProperty" -> cellData.value.unitCountProperty
                else -> null
            }
        }
    }

    private fun updateRow(row: RowData) {
        val data = if (row.classCode.isNotEmpty()) {
            classCodeMap[row.classCode]
        } else if (row.title.isNotEmpty()) {
            titleMap[row.title]
        } else {
            null
        }

        if (data != null) {
            row.title = data[1]
            row.time = data[2]
            row.day = data[3]
            row.room = data[4]
            row.unitCount = data[5]
        } else {
            clearDependentFields(row)
        }
    }

    private fun clearDependentFields(row: RowData) {
        row.title = ""
        row.time = ""
        row.day = ""
        row.room = ""
        row.unitCount = ""
    }

    @FXML
    fun handleAddRow() {
        subjectTable.items.add(RowData())
    }

    private fun loadCourseOptions(filePath: String) {
        try {
            val lines = File(filePath).readLines(StandardCharsets.UTF_8)
            courseOptions = lines.distinct()
        } catch (e: Exception) {
            println("Error loading courses.txt: ${e.message}")
            courseOptions = emptyList()
        }
    }

    private fun loadCsvData(filePath: String): List<List<String>> =
        File(filePath).readLines(StandardCharsets.UTF_8).map { it.split("\\|") } // Added UTF-8

    private fun updateUnitTotal() {
        val total = subjectTable.items.sumOf { row ->
            row.unitCount.toIntOrNull() ?: 0
        }
        totalUnitsField.text = total.toString()
    }

    private fun handleSubmission() {
        if (isAnyFieldEmpty()) {
            showAlert("Please fill in all fields.")
            return
        }

        val idNumber = idNumberTF.text
        val fullName = "${lastNameTF.text}, ${firstNameTF.text}, ${middleNameTF.text}"
        val course = courseComboBox.value
        val semester = semesterTF.text
        val schoolYear = schoolYearTF.text
        val date = datePicker.value.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) // Format date
        val yrLevel = yrLevelTF.text
        val selectedRadioButtonText = (toggleGroup.selectedToggle as RadioButton).text

        val classCodes = subjectTable.items.filter { it.classCode.isNotBlank() }.joinToString("|") { it.classCode }

        val csvLine = "$idNumber\\|$fullName\\|$course\\|$semester\\|$schoolYear\\|$date\\|$yrLevel\\|$selectedRadioButtonText\\|$classCodes"

        saveToCSV(csvLine)
        resetForm()
    }

    private fun isAnyFieldEmpty(): Boolean {
        return semesterTF.text.isBlank() || schoolYearTF.text.isBlank() ||
                datePicker.value == null || idNumberTF.text.isBlank() ||
                courseComboBox.value == null || yrLevelTF.text.isBlank() ||
                firstNameTF.text.isBlank() || lastNameTF.text.isBlank() ||
                middleNameTF.text.isBlank()
    }

    private fun showAlert(message: String) {
        val alert = Alert(Alert.AlertType.WARNING)
        alert.title = "Error"
        alert.headerText = null
        alert.contentText = message
        alert.showAndWait()
    }

    private fun saveToCSV(csvLine: String) {
        val filePath = "src/main/resources/com/example/regform/enrollees.csv"

        try {
            // Append the new line. If the file doesn't exist it will be created
            Files.writeString(Paths.get(File(filePath).absolutePath), csvLine + "\n", StandardCharsets.UTF_8,
                StandardOpenOption.CREATE, StandardOpenOption.APPEND)

        } catch (e: IOException) {
            showAlert("Error saving/appending to file: ${e.message}")
            e.printStackTrace()
        }
    }

    private fun resetForm() {
        semesterTF.clear()
        schoolYearTF.clear()
        datePicker.value = null
        idNumberTF.clear()
        courseComboBox.selectionModel.clearSelection()
        yrLevelTF.clear()
        firstNameTF.clear()
        lastNameTF.clear()
        middleNameTF.clear()
        newRbrn.isSelected = true // Reset to default radio button

        // Reset TableView
        subjectTable.items = FXCollections.observableArrayList()
        Platform.runLater { subjectTable.items.add(RowData()) }
    }

    private fun showPieChart() {
        val courseCounts = getCourseCounts(File("src/main/resources/com/example/regform/enrollees.csv").absolutePath)

        if (courseCounts.isEmpty()) {
            showAlert("No enrollment data available to create a chart.")
            return
        }

        val chartPane = createJavaFXPieChart(courseCounts) // Get the StackPane

        val stage = Stage()
        stage.initModality(Modality.APPLICATION_MODAL)
        stage.title = "Enrollment per Course"
        stage.scene = Scene(chartPane, 800.0, 500.0) // Set scene size
        stage.showAndWait()
    }

    private fun createJavaFXPieChart(data: Map<String, Int>): StackPane {
        val totalEnrollments = data.values.sum()
        val pieChartData = FXCollections.observableArrayList<PieChart.Data>()
        val decimalFormat = java.text.DecimalFormat("#.##")

        data.forEach { (course, count) ->
            val percentage = (count.toDouble() / totalEnrollments) * 100
            val formattedPercentage = decimalFormat.format(percentage)
            pieChartData.add(PieChart.Data("$course ($formattedPercentage%)", count.toDouble()))
        }

        val pieChart = PieChart(pieChartData)
        pieChart.title = "Enrollment per Course"
        pieChart.prefHeight = 800.0
        pieChart.prefWidth = 800.0

        val stackPane = StackPane(pieChart)
        stackPane.padding = Insets(20.0) // Add some padding around the chart
        return stackPane
    }

    private fun getCourseCounts(filePath: String): Map<String, Int> {
        if (!Files.exists(Paths.get(filePath))) {
            return emptyMap()
        }
        try {
            val lines = File(filePath).readLines()
            return lines.drop(0).groupingBy { it.split("\\|")[2] }.eachCount()
        } catch (e: IOException) {
            showAlert("Error reading enrollee data: ${e.message}")
            return emptyMap()
        }
    }
}