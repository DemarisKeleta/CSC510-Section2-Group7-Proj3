# 🍽️ Food Seer - Smart Meal Recommendation System 🍽️

[![Build & Test](https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3/actions/workflows/run-tests.yml/badge.svg?branch=Development-2)](https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3/actions/workflows/run-tests.yml?query=branch%3ADevelopment-2)
[![M0 Eval](https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3/actions/workflows/m0-eval.yml/badge.svg?branch=Development-2)](https://github.com/DemarisKeleta/CSC510-Section2-Group7-Proj3/actions/workflows/m0-eval.yml?query=branch%3ADevelopment-2)
[![License: MIT](https://img.shields.io/badge/license-MIT-yellow.svg)](LICENSE.md)

---

## 🆕 Project 2 (Fall 2026): what we added

Food Seer was built by a CSC510 team in Fall 2025 ([original repo](https://github.com/rishi082000/CSC510-Section2-Group7-Proj3)). In Fall 2026, Group 5 extended it with these milestones:

| Milestone | What it does | Docs |
| :- | :- | :- |
| **M0: risk-killer eval** | Measures whether allergy trace-back warns the right customers (recall) without warning everyone else (precision), on 500 simulated incidents. Runs in CI as **M0 Eval**. The claim currently fails (recall 0.890, precision 0.287), which triggered a spiral review. | [M0 section](docs/allergy-trace-back.md#m0-the-risk-killer-eval) |
| **Allergy incident trace-back** | When a restaurant or a customer reports an allergic reaction, Food Seer finds other customers who ate the same dishes in that time window and have a matching allergy, and sends each a private safety notice. Staff acknowledge the incident and attach an offer. | [docs/allergy-trace-back.md](docs/allergy-trace-back.md) |
| **Fitness-based calorie tracking** | Turns a customer's fitness data into a daily calorie budget, tracks calories from today's orders and logged meals, and recommends foods that fit what is left (Personal Diet page). | [docs/personal-diet.md](docs/personal-diet.md) |
| **Pantry-Paired Ordering** | Matches restaurant entrees with recipes based on ingredients the customer already has in their pantry. Shows ingredient-match percentages, missing ingredients, preparation time, and cooking instructions, while respecting dietary restrictions. Customers can select or skip suggested recipes. | [docs/docs/pantry-pairing.md](docs/docs/pantry-pairing.md) |

The **M0 Eval** badge above is red on purpose: it fails whenever the claim fails, so the number is honest and reproducible (`cd food-seer-backend && mvn -Dtest=M0TraceEval test`).

## 🎉 Welcome to Food Seer!

**Food Seer** takes the stress out of choosing what to eat by turning meal selection into a smart and personalized experience. Whether you're in the mood for a quick quiz or a natural conversation, Food Seer helps you find the perfect meal based on your preferences, mood, and budget. Start a short quiz or chat with our AI assistant — and receive a tailored meal suggestion in seconds.

**VIDEO**

[▶️ Watch the Demo Video](https://github.com/rishi082000/CSC510-Section2-Group7-Proj3/blob/main/doc/DEMO.mkv)

---

## Table of Contents

- [💡 Why This Project?](#-why-this-project)
- [📈 Use Case](#-use-case)
- [🚀 Key Features](#-key-features)
- [Technology Stack](#technology-stack)
- [🔧 Installation](#-installation)
- [🎥 Demo Video](#-demo-video)
- [📊 Project Poster](#-project-poster)
- [📚 Case Studies](#-case-studies)
- [🎓 Academic Paper Guidance](#-academic-paper-guidance)
- [🤝 Contribution Guidelines](#-contribution-guidelines)
- [📈 Code Coverage](#-code-coverage)
- [🌐 Future Scope](#-future-scope)
- [🙌 Contributors](#-contributors)
- [🙏 Acknowledgements](#-acknowledgements)
- [💡 Feedback](#-feedback)
- [🏛️ Governance Model](#governance-model)
- [🔔 Notifications](#-notifications)
- [📅 Roadmap & Funding Information](#-roadmap--funding-information)
- [🌟 Success Stories](#-success-stories)
- [📎 Recommended Citation](#-recommended-citation)
- [⚠️ Disclaimer](#️-disclaimer)

---

## 💡 Why This Project?

Menu overload is real — and decision fatigue hits hard when you're hungry. Food Seer eliminates the guesswork by guiding users toward a meal that fits their lifestyle and cravings without the hassle of scrolling through endless options.

We built Food Seer to:

- ✅ Reduce menu fatigue and decision stress  
- 🤖 Provide personalized suggestions via **AI chat**  
- ❓ Offer a quiz-based food recommendation system  
- 🛒 Streamline ordering into a simple “choose and order” flow  
- 🧾 Empower staff with inventory, user, and order management tools
- 🚚 Enable drivers to select orders with transparent compensation and earnings tracking

Food Seer bridges **AI-driven personalization** with **restaurant efficiency** — improving both customer experience and backend operations.

---

## 📈 Use Cases

### 1. User
The core customer experience centers on choice and feedback.
* **Direct Ordering:** Users can order food directly by filtering based on **allergies** and **food ratings**.
* **AI Chatbot:** Users can interact with an improved AI chatbot to receive personalized meal recommendations.
* **Mood Quiz:** Users can take a quick interactive quiz to get recommendations based on their current mood.
* **Rating System:** Once food is delivered, users can rate their meal to provide feedback.

### 2. Admin
Admins manage the platform using two specific dashboards.
* **Order Dashboard:** Admins can view all incoming orders and mark them as **fulfilled**.
* **Food Dashboard:** Admins can manage the food inventory and update stock levels/menu items.

### 3. Driver
Drivers have autonomy over their deliveries and earnings visibility.
* **Order Selection:** Drivers can view available orders along with the specific **compensation** for each.
* **Delivery Workflow:** Drivers can choose to accept an order and, upon completion, mark it as **delivered**.
* **Earnings:** Drivers can view their **total compensation** accrued over time.

---

## 🚀 Key Features

- **Interactive Quiz System** to generate meal recommendations  
- **Ollama-powered AI chatbot** for conversational food suggestions  
- **Clean, tab-based UI** for seamless navigation between ordering, recommendations, and account features  
- **Back-end systems** for inventory, ordering, session management, and user accounts  
- **Automated tests** with **85%+ coverage** across frontend & backend  

---

## Technology Stack

<code><a href="https://www.javascript.com/" target="_blank"><img height="50" src="https://cdn.freelogovectors.net/wp-content/uploads/2020/11/javascript_logo-768x873.png"></a></code>
<code><a href="https://developer.mozilla.org/en-US/docs/Glossary/HTML5" target="_blank"><img height="50" src="https://cdn.pixabay.com/photo/2017/08/05/11/16/logo-2582748_1280.png"></a></code>
<code><a href="https://www.java.com/" target="_blank"><img height="50" src="https://www.vectorlogo.zone/logos/java/java-icon.svg"></a></code>
<code><a href="https://www.mysql.com/" target="_blank"><img height="50" src="https://www.svgrepo.com/show/303251/mysql-logo.svg"></a></code>
<code><a href="https://www.npmjs.com/" target="_blank"><img height="50" src="https://www.svgrepo.com/show/452077/npm.svg"></a></code>
<code><a href="https://react.dev/" target="_blank"><img height="50" src="https://www.svgrepo.com/show/452092/react.svg"></a></code>
<code><a href="https://spring.io/projects/spring-boot" target="_blank"><img height="50" src="https://spring.io/img/projects/spring-boot.svg"></a></code>
<code><a href="https://ollama.com" target="_blank"><img height="50" src="https://cdn.jsdelivr.net/gh/homarr-labs/dashboard-icons/svg/ollama.svg"></a></code>

---

## 🔧 Installation

To set up the project locally, follow these steps.


**Clone the Repository:**
```bash
git https://github.com/rishi082000/CSC510-Section2-Group7-Proj3.git
```
Choose Your Installation Method. Detailed instructions are available in [INSTALL.md](INSTALL.md).

---

## 🎥 Demo Video
Check out our demo video to see the application in action! Click the link below to watch:

[▶️ Watch the Demo Video](https://github.com/rishi082000/CSC510-Section2-Group7-Proj3/blob/main/doc/DEMO.mkv)

---

## 📊 Project Poster
Curious about our project? View our project poster showcasing key aspects of the system [here](https://github.com/rishi082000/CSC510-Section2-Group7-Proj3/blob/main/doc/Section_2_Group_7_Project_3.pdf).

---

## 📚 Case Studies

Food Seer was used and evaluated in a controlled environment by our development team and peers.  
Through hands-on testing sessions, we observed that Food Seer improves food decision-making by:

- Reducing decision fatigue around choosing meals  
- Helping users quickly narrow down food options  
- Providing fast, personalized suggestions through quizzes and AI chat  
- Making the meal-selection process feel more guided and enjoyable  

Our internal testers consistently reported that Food Seer made deciding what to eat **faster**, **simpler**, and **less overwhelming**, especially when unsure where to start.

Additional user studies and data-driven evaluations are planned as the system matures.

---

## 🎓 Academic Paper Guidance

The guidance for Food Seer's chat bot was based on the influence on the following academic papers. These papers prompted ethical use and implementation of the chat bot for the best experience for users.

- [The influence of chatbot humour on consumer evaluations of services](https://onlinelibrary.wiley.com/doi/full/10.1111/ijcs.12849). - Hyunju Shin, Isabella Bunosso, Lindsay R. Levine
- [“I Am Here to Assist You Today”: The Role of Entity, Interactivity and Experiential Perceptions in Chatbot Persuasion](https://www.tandfonline.com/doi/abs/10.1080/08838151.2020.1834297?casa_token=w7sm6mnxsvMAAAAA:3MLFEo2kl-5tGra4ProYsyEMoFyJi3MOITlHtuhw8gBd0rTxLLS01ktiG63waepcvlAyx1n_uYZQ). - Carolin Ischen, Theo Araujo, Guda van Noort, Hilde Voorveld, Edith Smit
- [Understanding the impact of control levels over emotion-aware chatbots](https://www.sciencedirect.com/science/article/pii/S0747563221004453?casa_token=1ktZE4ekOqUAAAAA:5he3BwdaB_6WISF7UTxF9v2JO3KJ4eGiI3VbX48doIW7-s_XUhbHgWC0LoYT83UdYyxG8pHlDA). - Ivo Benke, Ulrich Gnewuch, Alexander Maedche 


---

## 🤝 Contribution Guidelines
The rules listed below are to be followed by the ones who will be contributing to the code in the repository:
 
- Atleast one review/approval is required from any other contributors of the project to merge a commit to the main branch.  
- It is recommended to delete the branch as soon as it is merged to the main branch to avoid stale branches in the repository.  
- It is encouraged to add name tags such as "feature/" or "patch/" in the branches if it is used to add code-patches or features in the project.

For more details on contributing to Food Seer, please read our full guide here: [CONTRIBUTING.md](./CONTRIBUTING.md)

---

---

## 📈 Code Coverage
[![codecov](https://codecov.io/gh/rishi082000/CSC510-Section2-Group7-Proj3/branch/main/graph/badge.svg)](https://app.codecov.io/github/rishi082000/CSC510-Section2-Group7-Proj3) 
---

## 🌐 Future Scope

We're excited about the enhancements coming to Food Seer! Here's what's next:

- **Integrate Maps and Live Order Location:**
  Incorporate real-time location tracking that could help customers stay informed about where their order is and provide admins with clearer delivery visibility.

- **Introduce Driver Ratings:**
  Allow both admins and customers to rate drivers on delivery time, service quality, and overall experience could help maintain a reliable and professional delivery ecosystem.

- **Support Multi-Order Pickup for Nearby Deliveries:**
   Give drivers the option to accept multiple orders within close proximity might streamline delivery routes and reduce wait times. 

- **Voice-Enabled Chat Experience:**  
  Add speech-to-text and text-to-speech so users can talk to the chatbot like a real waiter and receive spoken, natural meal recommendations — making the experience hands-free and more conversational.

---

## 🙌 Contributors
A heartfelt thank you to our contributors who made this project possible:

**Group #13**

- [Justin Kuethe](https://github.com/NovaCorz) - jrkuethe  
- [Chase Goins](https://github.com/jcgoins2) - jcgoins2  
- [Andrew Parr](https://github.com/afluffybunny15) - aaparr  
- [Mukul Sauhta](https://github.com/muksaw) - msauhta2

**Group #7(Section 2)**

- [Rishi Senthil Kumar](https://github.com/Rish1SK) - rsenthi  
- [Rishabh Ravi Kumar](https://github.com/rishi082000)- rishabh 
- [Jagadeshwar Muthukumaran](https://github.com/Jagadesh001ops) - jmuthuk  
- [Krisha Darji](https://github.com/KrishaDarji) - kdarji


**Group #5**

- [Demaris Ghide](https://github.com/DemarisKeleta) - gdemarisk
- [Neelan ](https://github.com/neelansp)   
- [Tierra Ingram](https://github.com/Tringram1)  

---

## 🙏 Acknowledgements
We extend our heartfelt thanks to Professor Dr. Timothy Menzies for his invaluable guidance in shaping our understanding of effective Software Engineering practices. We’re also deeply grateful to the teaching assistants for their consistent support and encouragement throughout the project.

---

## 💡 Feedback
Your input matters to us! If you have ideas for enhancements or new features, feel free to open an issue or submit a pull request. Thoughtful feedback from contributors like you helps us improve and evolve.

---

## Governance Model

Food Seer is maintained as an open-source project by its core development team.  
Project decisions — including new features, bug fixes, and roadmap updates — are made collaboratively through discussion and consensus among contributors.

While we do not currently have a formal governance structure, our guiding principles are:

- **Transparency:** All changes are proposed and reviewed publicly through GitHub pull requests.  
- **Collaboration:** Contributions are encouraged from all team members and external participants.  
- **Consensus-based Decision-Making:** Updates and feature changes are approved when all active contributors agree on the direction.  
- **Future Growth:** As the project expands and attracts more contributors, we plan to introduce a clearer governance model outlining maintainer roles, voting processes, and long-term project stewardship.

This approach ensures Food Seer remains both open and adaptable while maintaining high standards for code quality and collaboration.

---

## 🔔 Notifications

Users can subscribe to notifications to track changes to the Food Seer repository.  
To follow updates, visit the repo and click:

**Watch → All Activity**

This sends alerts for code updates, pull requests, issues, releases, and discussions.

---

## 📅 Funding Information

### Funding & Support
This project is **self-developed and unfunded**, created as part of academic coursework.  
- No external financial sponsors  
- Maintained by the student development team  
- Future support may include community contributions or academic extensions  

---

## 📎 Recommended Citation

If you use Food Seer in academic work, research, or presentations, please cite our project as follows:

**APA Format**
> Senthil Kumar, R. and Ravi Kumar, R. and Muthukumaran, J. and Darji, K. (2025). *Food Seer: Smart Meal Recommendation System*. GitHub Repository. https://github.com/rishi082000/CSC510-Section2-Group7-Proj3

**BibTeX**
```bibtex
@software{foodseer2025,
  author       = {Senthil Kumar, Rishi and Ravi kumar, Rishabh and Muthukumaran, Jagadeshwar and Darji, Krisha},
  title        = {Food Seer: Smart Meal Recommendation System},
  year         = {2025},
  publisher    = {GitHub},
  url          = {[https://github.com/rishi082000/CSC510-Section2-Group7-Proj3]},
}
```

---

## ⚠️ Disclaimer
Food Seer is designed as a smart recommendation system for restaurant delivery services. All meal recommendations are suggestions based on user input and preferences. Individual dietary needs, allergies, and health conditions should always be considered when making food choices. We are not responsible for any adverse reactions or health issues that may arise from following our recommendations.
