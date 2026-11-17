# ollama
offline model to build AI assistants

**Prerequisites & Local Setup**

**1. System Requirements**

Java: JDK 21 LTS

Build Tool: Maven 3.9+ (or bundled Maven wrapper)

LLM Runtime: Ollama (runs locally on port 11434)

**2. Install & Configure Ollama (Local LLM)**
This project runs completely free and offline using Ollama:

**Download the Installer:**

Visit the official Ollama download page: **https://ollama.com/download**

Download and run the installer for your OS (Windows, macOS, or Linux).

Once installed, Ollama runs silently in the background (check the system tray icon on Windows/macOS) and exposes a local REST API at http://localhost:11434


**Pull the Required Chat Model:**
ollama pull llama3.2:3b

**Test:**
Get: http://localhost:11434/api/tags
