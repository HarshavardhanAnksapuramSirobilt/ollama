# Day 5: Hybrid PDF + OCR RAG Pipeline (`pgvector` & Spring AI)

## Infrastructure & Dependencies Installed
* **Docker PostgreSQL + `pgvector` (`pgvector/pgvector:pg16`):** Running on host port `5434` (`hospital_db`) to store 768-dimensional vector embeddings with HNSW cosine similarity indexing.

docker run -d --name hospital-pgvector -e POSTGRES_USER=hospital -e POSTGRES_PASSWORD=hospital -e POSTGRES_DB=hospital_db -p 5434:5432 pgvector/pgvector:pg16



* **Ollama Embedding Model (`nomic-embed-text`):** Local embedding model used to convert PDF chunks and user queries into semantic vectors.

Pull the local embedding model (nomic-embed-text):
ollama pull nomic-embed-text


* **Maven Dependencies (`pom.xml`):**
    * `spring-ai-starter-vector-store-pgvector` & `spring-ai-vector-store-advisor` (`QuestionAnswerAdvisor` for retrieval-augmented generation).
    * `spring-ai-pdf-document-reader` (Apache PDFBox text extraction) + `rapidocr` / `rapidocr-onnx-platform` / `rapidocr-onnx-models` (zero-installer ONNX OCR fallback for scanned prescriptions).

## Day 5 Files Overview
* **`PdfUploadResponse5.java` (`dto`):** Java record DTO returning the uploaded file name, total pages read, vector chunks stored, and ingestion status.
* **`PdfIngestionService5.java` (`service`):** Hybrid ingestion pipeline that extracts text via `PagePdfDocumentReader`, falls back to RapidOCR (`ONNX_PPOCR_V3`) for scanned/image pages, splits text using `TokenTextSplitter`, and stores embeddings in `pgvector` while tracking the last uploaded file.
* **`HospitalPolicyService5.java` (`service`):** Configures `ChatClient` with `QuestionAnswerAdvisor` (scoped via `FILTER_EXPRESSION` to the target PDF) and `HospitalGuardrailAdvisor4` to answer questions strictly from uploaded documents without hallucination.
* **`HospitalPolicyController5.java` (`controller`):** Exposes `POST /api/v5/policies/upload` for multipart PDF ingestion and `GET /api/v5/policies/ask` for querying the last uploaded (or specified) document.

Api calls:
1)POST http://localhost:8080/api/v5/policies/upload
file (upload file)(body-> formdata-> key file  and upload file in value)
Response:
{
"fileName": "General_Medicine_Prescription_180926_Back_Pain_20260918_113328.pdf",
"pagesRead": 1,
"chunksStored": 1,
"status": "EMBEDDED_AND_STORED"
}


2)Get:
http://localhost:8080/api/v5/policies/ask?question=What is the hospital name, doctor name, and prescribed medicines in this prescription?
Response:
{
"answer": "I can answer the question based on the provided context.\n\nThe hospital name is not explicitly mentioned in the context. However, the clinic details are provided as follows:\n\n- Clinic Name: Pomnlun PHC\n- District: East Khasi Hills\n\nThe doctor name is not explicitly mentioned in the context. However, the name of the consulting doctor is mentioned as \"H T\".\n\nThe prescribed medicines are:\n\n- PARACETAMOL TABLET 650 MG Oral Solid Oral Every 12 hours For 2 days after meals",
"question": "What is the hospital name, doctor name, and prescribed medicines in this prescription?",
"searchedFile": "General_Medicine_Prescription_180926_Back_Pain_20260918_113328.pdf"
}