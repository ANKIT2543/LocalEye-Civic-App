# LocalEye 📍 - Civic Issue Reporting App

A location-based Android application that helps citizens report, track, and route local infrastructure issues to the proper municipal authorities. 

## 📱 App Features

### 1. Interactive Map & Live Feed
The app opens to a map built with OpenStreetMap, allowing users to see what is happening in their neighborhood. The community feed updates using a real-time database connection.

![home_page_1](https://github.com/user-attachments/assets/a4fa9ecb-cdce-4cae-a996-f00fdaa8d203)

![home_page_2](https://github.com/user-attachments/assets/430953c3-9004-41c8-bf90-21376dabc326)


---

### 2. Community Upvoting & Sorting
Users can upvote issues that impact them. Once a report receives 10 upvotes, the app automatically moves it to the top of the feed to highlight it as a high priority.

![sorting_gif](https://github.com/user-attachments/assets/e96ae2f4-a5e1-412e-8e26-399179708f87)


---

### 3. Issue Routing & Twitter Escalation
When a user selects a category (like Potholes or Garbage), the app automatically notes the relevant local authority on the details page. It also includes an option to draft a pre-filled tweet with the issue's location and government tags for easy escalation.

![report_details_1](https://github.com/user-attachments/assets/6cb116c6-2866-4027-9e30-1b105dafa583)

![report_details_2](https://github.com/user-attachments/assets/2d0e1dd5-54f3-4cd4-985e-74c8799f326a)

![twitter_escalate_gif](https://github.com/user-attachments/assets/737a91b7-b89c-4772-94b4-3ef8b0512da0)


---

### 4. Issue Reporting Form
A standard submission form that captures the issue title, category, precise GPS location, and an optional image for evidence.

![report_page](https://github.com/user-attachments/assets/03639e4a-181f-42c6-9caf-83cff3dae04c)


---

### 5. App Settings & Preferences
Built a proper settings screen to manage user accounts and application preferences. This includes a fully functional dark mode toggle that seamlessly updates the app's overall theme without restarting the application.

![dark_mode_gif](https://github.com/user-attachments/assets/57743194-05bb-49ce-95cb-d4748f59511f)


---

## 🛠️ Built With

**Frontend & Architecture**
* **Language:** Java, XML
* **Framework:** Android SDK
* **Architecture:** Android Navigation Component (Single-Activity Architecture)
* **UI/UX:** Material Design UI, RecyclerView, SharedPreferences

**Backend & Database**
* **Database:** Firebase Firestore (Real-time NoSQL)
* **Security:** Firebase Authentication (Secure Login & User Sessions)

**Location & Mapping**
* **Map Engine:** OSMDroid (OpenStreetMap API integration)
* **Location Services:** Google Fused Location Provider Client, Android Geocoder

**Media & External APIs**
* **Image Processing:** Glide (Image Caching & Loading), Cloudinary API
* **System Integrations:** Implicit Android Web Intents (Twitter/X escalation)
