# Real Time News Sentiment Prediction

## Overview
This project compares traditional supervised learning methods (Logistic Regression, Random Forest) with deep learning techniques (LSTM) for sentiment analysis of news headlines. The implementation includes real-time data streaming using Kafka and sentiment classification using various approaches.

## Table of Contents
- [Abstract](#abstract)
- [Introduction](#introduction)
- [Literature Review](#literature-review)
- [Methodology](#methodology)
- [Results](#results)
- [Conclusion](#conclusion)
- [Team](#team)

## Abstract
The project explores two distinct approaches to news headline sentiment analysis:
1. Traditional supervised learning methods (Logistic Regression and Random Forest)
2. Deep learning techniques (LSTM)

Traditional methods utilize feature extraction like TF-IDF, while deep learning leverages word embeddings and sequence modeling. The analysis evaluates performance, advantages, and limitations of each approach, highlighting deep learning models' superior ability to capture sentiment nuances while acknowledging the simplicity and speed of classic methods.

## Introduction
Sentiment analysis, a subset of natural language processing (NLP), determines the emotional tone behind text. News headlines serve as critical indicators of public sentiment due to their concise and impactful nature. The project focuses on classifying headlines as positive, negative, or neutral to provide valuable insights into prevailing sentiments regarding specific events, topics, or individuals.

### Key Points
- Traditional methods use supervised learning with labeled datasets
- Popular algorithms include Logistic Regression and Random Forest
- Focus on balancing interpretability and efficiency
- Analysis of strengths and limitations in context of news headlines

## Literature Review

### Traditional Supervised Learning Approaches
1. **Logistic Regression Features:**
   - Probabilistic Framework
   - Feature Extraction using Bag-of-Words/TF-IDF
   - High Interpretability
   - Competitive Performance
   - Known Limitations with Non-linear Relationships

### Challenges in Sentiment Analysis
1. Data Scarcity
2. Bias in Training Data
3. Complexity of Language
4. Feature Selection Complexity

### Future Directions
1. Hybrid Models Integration
2. Unsupervised Learning Exploration
3. External Factors Incorporation
4. Multi-language Adaptation
5. Bias Mitigation Strategies

## Methodology

### 1. Web Scraping
- Setup using JSoup for HTML parsing
- HTTP request handling
- Data extraction and storage
- Pagination handling

### 2. Kafka Producer (Data Streaming)
```python
# Key Components:
- KafkaProducer initialization
- CSV data source integration
- Iterative data sending
- Connection management
```

### 3. Kafka Consumer (Data Retrieval)
```python
# Key Features:
- Message listening
- JSON parsing
- TF-IDF vectorization
- Real-time prediction
```

### 4. Snorkel Labeling Technique
- Development of heuristics and programmatic rules
- Classification into positive (1) and negative (0)
- Integration with TextBlob
- LabelModel implementation

### 5. Logistic Regression Implementation
1. **Text Pre-processing:**
   - Tokenization
   - Lemmatization
   - Stop word removal
   - Punctuation removal

2. **Text Representation:**
   - TF-IDF vectorization
   - Feature weight assignment

3. **Model Training:**
   - Dataset splitting
   - Model fitting
   - Accuracy evaluation (92% achieved)

## Conclusion
The project successfully implemented a sentiment classifier for news headlines using Logistic Regression, achieving a 92% accuracy rate. The approach proves ideal for smaller datasets, offering strong performance without the computational requirements of deep learning models.


## References
- [Sentiment Analysis on News Headlines](https://towardsdatascience.com/sentiment-analysis-on-news-headlines-classic-supervised-learning-vs-deep-learning-approach-831ac698e276)
