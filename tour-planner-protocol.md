# Tour Planner Application Protocol

## 1. Introduction

The Tour Planner is a JavaFX desktop application developed using the Model-View-ViewModel (MVVM) architectural pattern. It allows users to plan, document, and analyze tours (trips) with detailed information and logging capabilities. This document describes the user experience design and includes analysis of the application wireframes.

## 2. System Architecture Overview

The Tour Planner application follows the MVVM pattern with the following components:

- **Models**: Represent the data structures (Tour, TourLog)
- **ViewModels**: Handle the application logic and state management
- **Views**: The user interface components defined in FXML
- **Services**: Business logic layer for tour and tour log operations

## 3. User Interface Components

### 3.1 Main Application Window

The main application window is divided into several key areas:
![Bildschirmfoto 2025-03-23 um 22.29.00.png](Bildschirmfoto%202025-03-23%20um%2022.29.00.png)

1. **Menu Bar**: Contains File, Options, and Help menus
2. **Toolbar**: Quick access buttons for common operations
3. **Tour List**: Left sidebar displaying all available tours
4. **Tour Details Area**: Central area showing details of the selected tour
5. **Tour Logs Table**: Bottom area displaying logs for the selected tour
6. **Status Bar**: Shows the current application status

### 3.2 Menu Structure

- **File Menu**:
  - Import: Import tours from external sources //work in progress
  - Export: Export tours to external formats //work in progress
  - Exit: Close the application
  
- **Options Menu**:
  - Preferences: Configure application settings
  
- **Help Menu**:
  - About: Display information about the application

### 3.3 Toolbar

The toolbar provides quick access to the most common operations:
- New Tour: Create a new tour
- Edit Tour: Modify the selected tour
- Delete Tour: Remove the selected tour
- New Log: Add a new log to the selected tour
- Search: Find tours based on various criteria

## 4. User Workflows

### 4.1 Tour Management

#### 4.1.1 Creating a New Tour

1. The user clicks the "New Tour" button in the toolbar or selects a similar option from a menu
2. A dialog appears with fields for tour details:
   - Name
   - From location
   - To location
   - Transport type (Car, Bicycle, Walking, Public Transport, Other)
   - Description
   - Distance (km)
   - Estimated time (minutes)
3. The user fills in the necessary details
4. The user can optionally click "Calculate Route" to automatically determine distance and time // work in progress
5. The user clicks "Save" to create the tour or "Cancel" to abort
6. If saved, the new tour appears in the tour list and is selected automatically

#### 4.1.2 Editing a Tour

1. The user selects a tour from the list
2. The user clicks the "Edit Tour" button
3. A dialog appears with the current tour details pre-filled
4. The user modifies the desired fields
5. The user clicks "Save" to apply changes or "Cancel" to discard them
6. The tour list and details display are updated to reflect any changes

#### 4.1.3 Deleting a Tour

1. The user selects a tour from the list
2. The user clicks the "Delete Tour" button
3. A confirmation dialog appears
4. If confirmed, the tour is removed from the list and any associated logs are deleted

### 4.2 Tour Log Management

#### 4.2.1 Creating a New Tour Log

1. The user selects a tour from the list
2. The user clicks the "New Log" button
3. A dialog appears with fields for log details:
   - Date and time
   - Total time spent (hours:minutes)
   - Total distance covered (km)
   - Difficulty rating (1-10 scale)
   - Overall rating (1-5 scale)
   - Comment/notes
4. The user fills in the log details
5. The user clicks "Save" to create the log or "Cancel" to abort
6. If saved, the new log appears in the tour logs table

#### 4.2.2 Editing a Tour Log (To Be Implemented)

1. The user selects a tour from the list
2. The user selects a log entry from the tour logs table
3. The user clicks an "Edit Log" button (to be added)
4. A dialog appears with the current log details pre-filled
5. The user modifies the desired fields
6. The user clicks "Save" to apply changes or "Cancel" to discard them
7. The logs table is updated to reflect any changes

#### 4.2.3 Deleting a Tour Log (To Be Implemented)

1. The user selects a tour from the list
2. The user selects a log entry from the tour logs table
3. The user clicks a "Delete Log" button (to be added)
4. A confirmation dialog appears
5. If confirmed, the log is removed from the table

### 4.3 Search Functionality

1. The user enters a search term in the search field
2. The tour list automatically updates to display only tours matching the search criteria
3. The search includes tour names, descriptions, locations, and log comments
4. Clearing the search field shows all tours again

### 4.4 Report Generation

1. The user selects a tour from the list
2. The user clicks the "Report" button
3. A detailed PDF report is generated with all tour information and logs
4. The report is displayed or saved according to user preference

### 4.5 Summary Generation

1. The user clicks the "Summary" button
2. A summary report is generated containing aggregated statistics about all tours
3. The summary is displayed or saved according to user preference

## 5. Data Display

### 5.1 Tour Details Display

For each selected tour, the following information is displayed:
- Tour name
- Start and end locations
- Distance (in kilometers)
- Estimated time (in hours:minutes format)
- Transport type
- Description
- A map visualization of the route (placeholder in the wireframe)

### 5.2 Tour Logs Table

The tour logs table displays the following columns:
- Date and time of the log entry
- Total time taken (hours:minutes)
- Distance covered (km)
- Difficulty rating (1-10)
- Overall rating (1-5)
- Comments/notes

## 6. Feature Enhancements and Missing Functionality

Based on the current implementation status, the following enhancements should be made:

1. **Edit Tour Log Functionality**: Implement the ability to edit existing tour logs
2. **Delete Tour Log Functionality**: Implement the ability to delete tour logs
3. **Map Integration**: Replace the map placeholder with an actual map showing the route
4. **Route Calculation**: Implement the route calculation functionality to automatically determine distance and time
5. **Report/Summary Generation**: Implement the report and summary generation features

## 7. Usability Considerations

### 7.1 Form Validation

- All required fields are validated before submission
- Numeric fields only accept valid numeric input
- Appropriate error messages are displayed for invalid input

### 7.2 Navigation and Selection

- Tours are selected by clicking on them in the tour list
- Selected tour's details and logs are automatically displayed
- Selection state is visually indicated

### 7.3 Responsive Design

- A Few components resize appropriately when the window is resized, the rest is fixed for a specific size
- The interface maintains usability at different window sizes (but there are limits)

## 8. Conclusion

The Tour Planner application provides a comprehensive solution for planning and documenting tours. The wireframe demonstrates a well-structured interface that follows established UI patterns. Once the missing functionality is implemented, the application will offer a complete user experience for tour management.

The next development phase should focus on implementing the missing features (Edit Tour Log, Delete Tour Log) and enhancing the existing functionality with proper map integration and automated route calculation.
