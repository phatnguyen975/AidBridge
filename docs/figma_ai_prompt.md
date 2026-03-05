# Figma AI Prompt: Disaster Relief Coordinator (DRC) App

## System Context

Design a clean, accessible, and high-contrast Android mobile application named "Disaster Relief Coordinator" (DRC). The app is used during natural disasters to connect Victims, Volunteers, and Sponsors. The design system must be native Android (Material Design 3). The color palette should use Red/Orange to indicate urgency or emergency (SOS), Green for safety/hubs, and clean white/gray backgrounds for readability under stressful conditions. Typography must be large, highly legible, and accessible.

## Screen 1: Onboarding & Role Selection

- **Header:** App logo (DRC) and a reassuring welcome message.
- **Content:** 4 large, distinct cards or buttons for user roles: "I need help (Victim)", "I want to help (Volunteer)", "I want to donate (Sponsor)", and "Quick SOS (Guest)".
- **UI Components:** Include simple vector icons for each role. The "Quick SOS" button should be distinct, large, and red.

## Screen 2: Guest / Victim - Main Public Map & Quick SOS

- **Top half:** A full-width Google Maps view showing "Hub Markers" (Green house icon), "Shelter Markers" (Flag icon), "Safe Path" polylines (Green line), and a red "SOS Heatmap".
- **Bottom half:** A massive, easily tappable circular red "SOS" button.
- **Form overlay (Hidden state):** When SOS is tapped, show a bottom sheet with input fields for: Name, Phone, Details (house description, health status), Image upload button, and an auto-captured GPS coordinate text.

## Screen 3: Victim - Supply Request Dashboard

- **Header:** "Request Supplies" title.
- **Content:** A grid layout showing 5 main supply categories with icons: Medicine, Clothes, Food, Water, and Other.
- **UI Components:** Next to each category, add a number stepper (+/- buttons) to specify the number of Adults, Elders, and Children. Include a prominent "AI Voice Support" floating action button (microphone icon) for hands-free input.

## Screen 4: Volunteer - Mission Alert & Active Routing

- **Popup overlay:** A high-alert modal showing "New Mission Dispatch" with a visible 30-second countdown timer, an "Accept" (Green) button, and a "Decline" (Red) button.
- **Main Screen (Active Mission):** A map showing an optimized route polyline.
- **Bottom Sheet:** A "Status Bar" showing steps (Go to Hub -> Pick up -> Go to Victim). Below that, a Victim Info Card displaying Name, Phone, Location notes, and a "Chat/Call" button. Include an "Online/Offline" toggle switch at the top app bar.

## Screen 5: Sponsor - Donation Form & QR Generation

- **Top section:** Dropdown or chips to select item categories (Medicine, Clothes, Food, Water) and input fields for quantity and expiry date.
- **Map section:** A small map snippet showing "Smart Hub Selection" (suggesting the nearest hub lacking supplies).
- **Bottom section:** A large, scannable QR Code generated for the donation, with a "Confirm Drop-off" status text.

## Screen 6: Hub Staff - QR Scanner & Inventory

- **Main Screen:** A full-screen camera view for scanning QR codes.
- **Bottom UI:** Two tabs to switch between "Inbound (Sponsor)" and "Outbound (Volunteer)" modes.
- **Overlay Card:** After scanning, show a success confirmation card with the item name, quantity, and real-time updated inventory count (Atomic Inventory Update indicator). Include an "Emergency Hub Status" toggle (Open/Closed due to hazard).
