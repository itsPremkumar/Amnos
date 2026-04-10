# Contributing to Amnos

First off, thank you for considering contributing to **Amnos**! It's people like you that make the open-source privacy community so strong.

## 🛡️ Security First
Amnos is a security-critical application. Before submitting any changes, please ensure:
- Your code does not introduce persistent storage (No `SharedPreferences`, `SQL`, or `Disk Cache`).
- You are not using any external trackers or analytics.
- All network requests follow the `NetworkSecurityManager` hardening rules.

## 🚀 How Can I Contribute?

### Reporting Bugs
- Use the **GitHub Issues** tab.
- Provide a clear, descriptive title.
- Describe the steps to reproduce the bug.
- Include your Android version and device model.

### Suggesting Enhancements
- Open an Issue with the tag `enhancement`.
- Explain how the feature improves user privacy or performance.

### Pull Requests
1. **Fork** the repo.
2. **Create a branch** for your feature (`git checkout -b feature/amazing-feature`).
3. **Commit** your changes (`git commit -m 'Add some amazing feature'`).
4. **Push** to the branch (`git push origin feature/amazing-feature`).
5. **Open a Pull Request**.

## 💻 Technical Standards
- **Language**: Kotlin.
- **UI Framework**: Jetpack Compose (Material 3).
- **Architecture**: Modular / Repository pattern.
- **Formatting**: Please follow standard Kotlin coding conventions.

## ⚖️ License
By contributing, you agree that your contributions will be licensed under its **MIT License**.
