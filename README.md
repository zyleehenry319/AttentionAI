# AttentionAI - AI-Powered Android Productivity App

A revolutionary Android productivity app that uses advanced AI to analyze your phone usage patterns, providing intelligent insights, personalized recommendations, and comprehensive activity summaries. The app can see and hear what's happening on your phone screen, then use OpenAI's GPT-4 to provide deep analysis and actionable advice.

## 🚀 Key Features

### 🤖 **AI-Powered Intelligence** (Main Feature)
- **OpenAI GPT-4 Integration**: Real AI analysis using the most advanced language model
- **Intelligent Question & Answer**: Ask any question about your phone activity and get AI-powered responses
- **Smart Summaries**: AI-generated comprehensive session summaries with insights and recommendations
- **Productivity Insights**: Deep behavioral analysis with personalized productivity scores
- **Custom Recommendations**: Actionable advice tailored to your specific usage patterns
- **Pattern Recognition**: AI identifies trends, distractions, and productivity opportunities

### 🎥 **Advanced Screen & Audio Capture**
- **Screen Recording**: Captures screen content using Android's MediaProjection API
- **Audio Recording**: Records microphone audio for comprehensive activity analysis
- **Background Processing**: Runs as a foreground service to ensure continuous recording
- **Text Recognition**: Uses ML Kit to extract text from screen content for AI analysis

### 📊 **Comprehensive Analytics**
- **Session Management**: Track recording sessions with timestamps and duration
- **App Usage Analytics**: Monitor which apps you use and for how long
- **AI Productivity Scoring**: Get intelligent productivity scores based on your behavior
- **Historical Data**: View past sessions and compare productivity over time
- **Real-time Insights**: Get AI analysis during or after your sessions

### 🎨 **Modern User Experience**
- **Material Design 3**: Beautiful, modern interface following Google's design guidelines
- **AI Configuration**: Easy setup for OpenAI API keys and AI parameters
- **Dark/Light Theme**: Automatic theme switching based on system preferences
- **Intuitive Controls**: Easy-to-use recording controls and AI features
- **Real-time Status**: Live updates on recording status and AI processing

## 🏗️ Technical Architecture

### Core Components
- **MainActivity**: Main entry point with permission handling and AI-powered UI
- **ScreenRecordingService**: Foreground service for screen and audio recording
- **AIProcessingService**: Background service for AI analysis and processing
- **AIService**: OpenAI integration service with advanced prompt engineering
- **ActivityRepository**: Data layer for managing sessions and AI insights
- **Room Database**: Local storage for sessions, events, and AI insights

### AI Integration
- **OpenAI GPT-4**: Advanced language model for intelligent analysis
- **Retrofit**: HTTP client for OpenAI API communication
- **Prompt Engineering**: Specialized prompts for different analysis types
- **Context Processing**: Comprehensive data preprocessing for AI
- **Response Parsing**: Intelligent parsing of AI responses

### Key Technologies
- **Kotlin**: Modern Android development with coroutines
- **Jetpack Compose**: Declarative UI framework with Material Design 3
- **Room Database**: Local data persistence with type converters
- **ML Kit**: On-device machine learning for text recognition
- **MediaProjection API**: Screen recording capabilities
- **MediaRecorder**: Audio recording functionality
- **Retrofit + Gson**: API communication and JSON parsing
- **SharedPreferences**: Secure API key storage

## Permissions Required

The app requires several permissions to function properly:

- **RECORD_AUDIO**: For microphone access during recording
- **WRITE_EXTERNAL_STORAGE**: For saving recorded files
- **READ_EXTERNAL_STORAGE**: For accessing saved recordings
- **FOREGROUND_SERVICE**: For background recording service
- **SYSTEM_ALERT_WINDOW**: For overlay controls (optional)
- **INTERNET**: For potential AI service integration

## 🚀 Installation & Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd AttentionAI
   ```

2. **Open in Android Studio**
   - Import the project
   - Sync Gradle files
   - Install required dependencies

3. **Configure AI Services** (Required for AI features)
   - Get your OpenAI API key from [platform.openai.com](https://platform.openai.com)
   - Open the app and go to Settings (gear icon)
   - Enter your API key and configure AI parameters
   - Choose your preferred model (GPT-4 recommended)

4. **Build and Run**
   - Connect an Android device (API 24+)
   - Build and install the app
   - Grant required permissions when prompted
   - Configure AI settings for full functionality

## 📱 Usage Guide

### 🤖 **AI-Powered Features** (Main Focus)

#### **Setting Up AI**
1. Open the app and tap the Settings (gear) icon
2. Enter your OpenAI API key
3. Choose your preferred model (GPT-4 recommended)
4. Configure AI parameters (temperature, max tokens)
5. Save configuration

#### **Asking AI Questions**
1. **Quick Questions**: Use preset buttons for common queries:
   - "What apps did I use today?"
   - "How productive was I?"
2. **Custom Questions**: Tap "Ask Custom Question" for specific queries:
   - "What was I doing between 2-3 PM?"
   - "How much time did I spend on social media?"
   - "What were my main distractions today?"
3. **Get AI Responses**: Receive intelligent, context-aware answers

#### **AI-Generated Summaries**
1. Tap "Generate Summary" for comprehensive session analysis
2. Get AI-powered insights including:
   - Key highlights and achievements
   - Productivity patterns and trends
   - Specific recommendations for improvement
   - Detailed app usage breakdown

#### **AI Insights & Recommendations**
1. Tap "Insights" to get deep behavioral analysis
2. Receive personalized recommendations
3. View productivity scores and key findings
4. Get actionable advice for improvement

### 🎥 **Recording & Data Collection**

#### **Starting a Recording Session**
1. Open the app and grant necessary permissions
2. Tap "Start Recording" to begin screen and audio capture
3. The app runs in the background as a foreground service
4. Use your phone normally - the app tracks everything

#### **Managing Sessions**
1. View active session duration in real-time
2. Stop recording when done
3. Access historical sessions and their AI analysis
4. Compare productivity across different sessions

## 🔒 Privacy & Security

### **Data Privacy**
- **Local Storage**: All recordings and raw data stay on your device
- **AI Data Sharing**: Only aggregated, anonymized data sent to OpenAI
- **No Personal Data**: AI receives app usage patterns, not personal content
- **Secure API Keys**: API keys stored securely in Android SharedPreferences
- **User Control**: Complete control over what data is shared with AI

### **AI Privacy Features**
- **Context Filtering**: Only relevant productivity data sent to AI
- **No Raw Content**: Screen recordings and audio stay local
- **Anonymized Analysis**: AI analyzes patterns, not personal information
- **Optional AI**: Can use app without AI features if preferred
- **Data Deletion**: Complete data deletion options available

### **Security Measures**
- **Encrypted Storage**: All local data encrypted
- **Permission Management**: Clear explanation of required permissions
- **Secure Communication**: HTTPS for all API communications
- **No Tracking**: No user tracking or analytics

## Data Management

- **Automatic Cleanup**: Old recordings are automatically cleaned up
- **Export Options**: Export summaries and insights
- **Data Deletion**: Complete data deletion options available
- **Storage Monitoring**: Track storage usage and manage space

## 🚀 Future Enhancements

### **AI & Intelligence**
- **Advanced AI Models**: Integration with GPT-5 and other cutting-edge models
- **Voice Commands**: Voice interaction with AI assistant
- **Predictive Analytics**: AI predictions about productivity patterns
- **Smart Scheduling**: AI-powered time management suggestions
- **Emotion Analysis**: AI analysis of mood and stress patterns

### **Productivity Features**
- **Goal Setting**: Set and track productivity goals with AI guidance
- **Team Analytics**: Shared productivity insights for teams
- **Smart Notifications**: Context-aware productivity reminders
- **Focus Modes**: AI-suggested focus sessions based on patterns
- **Habit Tracking**: AI-powered habit formation and tracking

### **Platform Expansion**
- **Wear OS Support**: Companion app for smartwatches
- **Desktop Integration**: Cross-platform productivity tracking
- **API Access**: Third-party integrations and custom workflows
- **Cloud Sync**: Optional cloud backup and sync (privacy-preserving)

### **Advanced Analytics**
- **Trend Analysis**: Long-term productivity trend analysis
- **Comparative Insights**: Compare productivity across time periods
- **Custom Dashboards**: Personalized analytics dashboards
- **Export Features**: Advanced data export and reporting

## Contributing

We welcome contributions! Please see our contributing guidelines for details on:
- Code style and standards
- Testing requirements
- Pull request process
- Issue reporting

## License

This project is licensed under the MIT License - see the LICENSE file for details.

## Support

For support, feature requests, or bug reports:
- Create an issue in the GitHub repository
- Contact the development team
- Check the documentation and FAQ

## 🎯 **Why AttentionAI?**

### **The Problem**
- **Digital Distraction**: We spend hours on our phones but don't understand our patterns
- **Productivity Blind Spots**: We can't see where our time actually goes
- **Lack of Insights**: Basic usage stats don't provide actionable advice
- **No Personalization**: Generic productivity advice doesn't fit individual needs

### **The Solution**
- **AI-Powered Analysis**: Deep understanding of your digital behavior patterns
- **Intelligent Insights**: Personalized recommendations based on your actual usage
- **Comprehensive Tracking**: See and hear everything that happens on your phone
- **Actionable Advice**: Specific, tailored recommendations for improvement

### **Key Benefits**
- **Understand Your Patterns**: AI reveals hidden productivity patterns
- **Get Personalized Advice**: Recommendations tailored to your specific behavior
- **Track Real Progress**: See actual improvements over time
- **Make Informed Decisions**: Data-driven insights for better choices
- **Improve Focus**: AI helps identify and reduce distractions

---

**Note**: This app is designed for personal productivity enhancement. Always respect privacy laws and regulations in your jurisdiction when using screen recording features. The AI features require an OpenAI API key and internet connection.
