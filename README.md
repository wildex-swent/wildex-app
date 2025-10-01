# Wildex

Wildex is a mobile application that helps users identify animals instantly using computer vision and geolocation. It is designed for both casual explorers and professionals, offering species recognition, collection management, mapping, and rescue reporting features.

## Pitch

Have you ever come across an animal you couldn’t identify and wished you had the answer right away? Wildex solves this problem by combining computer vision with mobile convenience: simply snap a photo and, within seconds, you’ll know exactly what animal you’re looking at. Beyond identification, the app allows you to build your own collection of animals, keep track of your sightings on a map, and share your discoveries with others. You can also subscribe to friends or experts to follow their journeys in the wild. For added impact, professional users receive rescue alerts when a hurt or wandering animal is reported nearby, creating a bridge between casual explorers and trained responders. The core audience includes curious nature lovers, hikers, and travelers who want to learn more about wildlife, as well as professionals such as biologists or veterinarians who can benefit from community-driven reporting.

## Features

* Instant animal identification using computer vision
* Personal animal collections and sighting history
* Interactive map of sightings with geolocation support
* Social features: follow friends, experts, and share discoveries
* Rescue alerts for professionals when animals are reported nearby
* Support for offline usage with automatic synchronization when online

## Architecture

Wildex follows a split-app model built on Firebase and cloud services:

* **Firebase Authentication**: Handles secure user sign-in and differentiates between regular and professional users.
* **Firestore Database**: Stores user collections, profiles, and reported cases of hurt or wandering animals.
* **Firebase Storage**: Manages storage and retrieval of uploaded animal photos.
* **Firebase Cloud Messaging**: Delivers notifications, including real-time rescue alerts for professionals.
* **Computer Vision Service**: Animal identification is powered by an external service, chosen based on performance and accuracy.

## Multi-User Support

* Users are authenticated via Firebase Authentication, currently supporting Google login.
* Each user has a dedicated profile containing their collections and sighting history.
* Role-based management distinguishes between:

  * Regular users: can collect, map, and share sightings.
  * Professional users: receive rescue alerts and community-driven reports.
* Permissions are enforced at the authentication and Firestore level to ensure proper access control.

## Sensor Integration

The application makes use of core device sensors to enable its main functionality:

* **Camera**: Captures animal photos for identification and collection.
* **GPS**: Records the location of each sighting, supports map visualization, and enables location-based alerts.

## Offline Mode

Wildex supports partial offline functionality to ensure continuity of use:

* Users can view their existing collections and sighting maps offline.
* New photos and sightings can be captured and stored locally.
* Once the device reconnects to the internet, pending uploads are synchronized automatically.
* Online-only features such as species recognition, rescue alerts, and social features are unavailable offline.

## Target Audience

Wildex is designed for:

* Nature enthusiasts and hikers who want to explore and learn about wildlife.
* Travelers and adventurers who encounter new species during their journeys.
* Professionals such as biologists, veterinarians, and rescue workers who benefit from real-time, community-driven animal reporting.

## Figma Design

The UI/UX design for Wildex is available on Figma:
https://www.figma.com/design/1JJDxqvf0pTM0Jg3E8u0oX/Wildex-App-Desgin?node-id=0-1&t=xND19ky1WJj4gFU2-1