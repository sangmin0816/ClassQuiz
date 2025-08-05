document.addEventListener('DOMContentLoaded', () => {
    const quizListSection = document.getElementById('quiz-list-section');
    const quizCreationSection = document.getElementById('quiz-creation-section');
    const questionCreationSection = document.getElementById('question-creation-section');

    const showQuizzesBtn = document.getElementById('show-quizzes');
    const showQuizCreationBtn = document.getElementById('show-quiz-creation');
    const showQuestionCreationBtn = document.getElementById('show-question-creation');

    const createQuizForm = document.getElementById('create-quiz-form');
    const createQuestionForm = document.getElementById('create-question-form');

    const questionTypeSelect = document.getElementById('question-type');
    const optionsContainer = document.getElementById('options-container');
    const subjectiveAnswerContainer = document.getElementById('subjective-answer-container');
    const addOptionBtn = document.getElementById('add-option-btn');
    const removeOptionBtn = document.getElementById('remove-option-btn');
    const optionInputsDiv = document.getElementById('option-inputs');
    const correctAnswerMultipleSelect = document.getElementById('correct-answer-multiple');

    const quizListDiv = document.getElementById('quiz-list');
    const addQuestionsToQuizArea = document.getElementById('add-questions-to-quiz-area');
    const selectQuizToAddQuestions = document.getElementById('select-quiz-to-add-questions');
    const availableQuestionsListDiv = document.getElementById('available-questions-list');
    const addSelectedQuestionsBtn = document.getElementById('add-selected-questions-btn');


    // --- 섹션 전환 함수 ---
    const showSection = (sectionToShow) => {
        [quizListSection, quizCreationSection, questionCreationSection].forEach(section => {
            section.style.display = 'none';
        });
        sectionToShow.style.display = 'block';
    };

    showQuizzesBtn.addEventListener('click', (e) => {
        e.preventDefault();
        showSection(quizListSection);
        loadQuizzes(); // 퀴즈 목록 로드
    });

    showQuizCreationBtn.addEventListener('click', (e) => {
        e.preventDefault();
        showSection(quizCreationSection);
        loadAllQuestionsForQuizAddition(); // 문제 추가를 위한 모든 문제 로드
        loadQuizzesForSelection(); // 문제 추가할 퀴즈 선택을 위한 퀴즈 목록 로드
    });

    showQuestionCreationBtn.addEventListener('click', (e) => {
        e.preventDefault();
        showSection(questionCreationSection);
        // 문제 유형에 따라 입력 필드 초기화
        toggleQuestionTypeFields();
        updateCorrectAnswerOptions();
    });

    // --- 퀴즈 목록 로드 ---
    const loadQuizzes = async () => {
        quizListDiv.innerHTML = '<p>퀴즈 목록을 로딩 중...</p>';
        try {
            const response = await fetch('/api/quizzes');
            const quizzes = await response.json();

            if (quizzes.length === 0) {
                quizListDiv.innerHTML = '<p>등록된 퀴즈가 없습니다. "퀴즈 생성" 탭에서 새로운 퀴즈를 만들어보세요!</p>';
                return;
            }

            quizListDiv.innerHTML = '';
            quizzes.forEach(quiz => {
                const quizElement = document.createElement('div');
                quizElement.innerHTML = `
                    <h3>${quiz.title}</h3>
                    <p>${quiz.description || '설명 없음'}</p>
                    <p>시작: ${quiz.startTime ? new Date(quiz.startTime).toLocaleString() : '미정'}</p>
                    <p>종료: ${quiz.endTime ? new Date(quiz.endTime).toLocaleString() : '미정'}</p>
                    <button class="view-quiz-detail-btn" data-quiz-id="${quiz.id}">문제 보기</button>
                    <button class="delete-quiz-btn" data-quiz-id="${quiz.id}">삭제</button>
                `;
                quizListDiv.appendChild(quizElement);
            });

            // "문제 보기" 버튼 이벤트 리스너 추가
            document.querySelectorAll('.view-quiz-detail-btn').forEach(button => {
                button.addEventListener('click', async (e) => {
                    const quizId = e.target.dataset.quizId;
                    alert(`퀴즈 ID: ${quizId} 문제 보기 기능은 아직 구현되지 않았습니다. (백엔드에서 퀴즈 조회 API는 있으니 콘솔에서 확인 가능)`);
                    // TODO: 나중에 이 부분에 특정 퀴즈의 문제를 불러와서 학생이 풀 수 있는 화면으로 전환하는 로직 추가
                });
            });

            // "삭제" 버튼 이벤트 리스너 추가
            document.querySelectorAll('.delete-quiz-btn').forEach(button => {
                button.addEventListener('click', async (e) => {
                    const quizId = e.target.dataset.quizId;
                    if (confirm(`정말로 퀴즈 "${quizId}"를 삭제하시겠습니까? (연결된 문제도 함께 삭제됩니다)`)) {
                        try {
                            const response = await fetch(`/api/quizzes/${quizId}`, {
                                method: 'DELETE'
                            });
                            if (response.ok) {
                                alert('퀴즈가 성공적으로 삭제되었습니다.');
                                loadQuizzes(); // 목록 새로고침
                            } else {
                                const errorText = await response.text();
                                alert(`퀴즈 삭제 실패: ${errorText || response.statusText}`);
                            }
                        } catch (error) {
                            console.error('퀴즈 삭제 중 오류 발생:', error);
                            alert('퀴즈 삭제 중 오류가 발생했습니다.');
                        }
                    }
                });
            });


        } catch (error) {
            console.error('퀴즈 목록 로드 중 오류 발생:', error);
            quizListDiv.innerHTML = '<p style="color: red;">퀴즈 목록을 불러오는 데 실패했습니다.</p>';
        }
    };

    // --- 새 퀴즈 생성 폼 제출 ---
    createQuizForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const title = document.getElementById('quiz-title').value;
        const description = document.getElementById('quiz-description').value;
        const startTime = document.getElementById('quiz-start-time').value;
        const endTime = document.getElementById('quiz-end-time').value;

        const quizData = {
            title,
            description,
            startTime: startTime ? new Date(startTime).toISOString() : null,
            endTime: endTime ? new Date(endTime).toISOString() : null
        };

        try {
            const response = await fetch('/api/quizzes', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(quizData)
            });

            if (response.ok) {
                const newQuiz = await response.json();
                alert(`퀴즈 "${newQuiz.title}" (ID: ${newQuiz.id})가 생성되었습니다! 이제 문제들을 이 퀴즈에 추가할 수 있습니다.`);
                createQuizForm.reset();
                addQuestionsToQuizArea.style.display = 'block'; // 퀴즈 생성 후 문제 추가 영역 표시
                loadAllQuestionsForQuizAddition(); // 문제 목록 새로고침
                loadQuizzesForSelection(); // 퀴즈 선택 드롭다운 새로고침
            } else {
                const errorText = await response.text();
                alert(`퀴즈 생성 실패: ${errorText || response.statusText}`);
            }
        } catch (error) {
            console.error('퀴즈 생성 중 오류 발생:', error);
            alert('퀴즈 생성 중 오류가 발생했습니다.');
        }
    });

    // --- 문제 생성 폼 관련 로직 ---
    const updateCorrectAnswerOptions = () => {
        correctAnswerMultipleSelect.innerHTML = ''; // 기존 옵션 초기화
        const optionInputs = document.querySelectorAll('.option-input');
        optionInputs.forEach((input, index) => {
            if (input.value.trim() !== '') { // 내용이 있는 선택지만 옵션으로 추가
                const optionElement = document.createElement('option');
                optionElement.value = input.value.trim(); // 실제 정답 텍스트를 값으로 사용
                optionElement.textContent = `선택지 ${index + 1}: ${input.value.trim()}`;
                correctAnswerMultipleSelect.appendChild(optionElement);
            }
        });
    };

    const toggleQuestionTypeFields = () => {
        if (questionTypeSelect.value === 'MULTIPLE_CHOICE') {
            optionsContainer.style.display = 'block';
            subjectiveAnswerContainer.style.display = 'none';
            updateCorrectAnswerOptions(); // 객관식일 때만 정답 선택지 업데이트
        } else {
            optionsContainer.style.display = 'none';
            subjectiveAnswerContainer.style.display = 'block';
        }
    };

    questionTypeSelect.addEventListener('change', toggleQuestionTypeFields);

    addOptionBtn.addEventListener('click', () => {
        if (optionInputsDiv.children.length / 2 < 10) { // input과 <br>이 한 쌍
            const input = document.createElement('input');
            input.type = 'text';
            input.className = 'option-input';
            input.placeholder = `선택지 ${optionInputsDiv.children.length / 2 + 1}`;
            input.addEventListener('input', updateCorrectAnswerOptions); // 입력 시마다 정답 옵션 업데이트
            optionInputsDiv.appendChild(input);
            optionInputsDiv.appendChild(document.createElement('br'));
            updateCorrectAnswerOptions();
        } else {
            alert('선택지는 최대 10개까지 추가할 수 있습니다.');
        }
    });

    removeOptionBtn.addEventListener('click', () => {
        const currentOptionCount = optionInputsDiv.children.length / 2;
        if (currentOptionCount > 2) {
            optionInputsDiv.removeChild(optionInputsDiv.lastChild); // <br> 제거
            optionInputsDiv.removeChild(optionInputsDiv.lastChild); // input 제거
            updateCorrectAnswerOptions();
        } else {
            alert('선택지는 최소 2개 이상이어야 합니다.');
        }
    });

    // 초기 로딩 시 기존 선택지 입력 필드에 이벤트 리스너 추가
    document.querySelectorAll('.option-input').forEach(input => {
        input.addEventListener('input', updateCorrectAnswerOptions);
    });

    // --- 새 문제 생성 폼 제출 ---
    createQuestionForm.addEventListener('submit', async (e) => {
        e.preventDefault();
        const content = document.getElementById('question-content').value;
        const type = questionTypeSelect.value;
        let options = [];
        let correctAnswerText = '';

        if (type === 'MULTIPLE_CHOICE') {
            document.querySelectorAll('.option-input').forEach(input => {
                if (input.value.trim() !== '') {
                    options.push(input.value.trim());
                }
            });
            correctAnswerText = correctAnswerMultipleSelect.value; // 선택된 옵션의 텍스트가 정답
            if (options.length < 2 || options.length > 10) {
                alert('객관식 문제는 2개에서 10개 사이의 선택지를 가져야 합니다.');
                return;
            }
            if (!correctAnswerText) {
                alert('객관식 문제의 정답을 선택해주세요.');
                return;
            }
        } else { // SUBJECTIVE
            correctAnswerText = document.getElementById('correct-answer-subjective').value.trim();
            if (!correctAnswerText) {
                alert('주관식 문제의 정답을 입력해주세요.');
                return;
            }
        }

        const questionData = {
            content,
            type,
            options,
            correctAnswerText
        };

        try {
            const response = await fetch('/api/questions', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(questionData)
            });

            if (response.ok) {
                const newQuestion = await response.json();
                alert(`문제 "${newQuestion.content.substring(0, 20)}..." (ID: ${newQuestion.id})가 생성되었습니다!`);
                createQuestionForm.reset();
                // 선지 입력 필드 초기화 (최소 2개만 남김)
                optionInputsDiv.innerHTML = `
                    <input type="text" class="option-input" placeholder="선택지 1"><br>
                    <input type="text" class="option-input" placeholder="선택지 2"><br>
                `;
                document.querySelectorAll('.option-input').forEach(input => {
                    input.addEventListener('input', updateCorrectAnswerOptions);
                });
                updateCorrectAnswerOptions(); // 정답 선택지도 초기화
                toggleQuestionTypeFields(); // 유형별 필드 초기화
                loadAllQuestionsForQuizAddition(); // 문제 추가 목록 새로고침
            } else {
                const errorText = await response.text();
                alert(`문제 생성 실패: ${errorText || response.statusText}`);
            }
        } catch (error) {
            console.error('문제 생성 중 오류 발생:', error);
            alert('문제 생성 중 오류가 발생했습니다.');
        }
    });

    // --- 퀴즈에 문제 추가 로직 ---
    const loadQuizzesForSelection = async () => {
        selectQuizToAddQuestions.innerHTML = '<option value="">퀴즈 선택</option>';
        try {
            const response = await fetch('/api/quizzes');
            const quizzes = await response.json();
            quizzes.forEach(quiz => {
                const option = document.createElement('option');
                option.value = quiz.id;
                option.textContent = `${quiz.title} (ID: ${quiz.id})`;
                selectQuizToAddQuestions.appendChild(option);
            });
        } catch (error) {
            console.error('퀴즈 목록 로드 중 오류 발생:', error);
            alert('문제 추가를 위한 퀴즈 목록 로드 실패.');
        }
    };

    const loadAllQuestionsForQuizAddition = async () => {
        availableQuestionsListDiv.innerHTML = '<p>문제 목록을 로딩 중...</p>';
        try {
            const response = await fetch('/api/questions');
            const questions = await response.json();

            if (questions.length === 0) {
                availableQuestionsListDiv.innerHTML = '<p>생성된 문제가 없습니다. "문제 생성" 탭에서 문제를 만들어보세요!</p>';
                return;
            }

            availableQuestionsListDiv.innerHTML = '';
            questions.forEach(question => {
                const questionElement = document.createElement('div');
                questionElement.className = 'question-item';
                questionElement.innerHTML = `
                    <label>
                        <input type="checkbox" class="question-checkbox" value="${question.id}">
                        문제 ID: ${question.id} | 유형: ${question.type} | 내용: ${question.content.substring(0, 50)}...
                        ${question.quiz ? `(현재 퀴즈 ID: ${question.quiz.id})` : '(퀴즈 미연결)'}
                    </label>
                `;
                availableQuestionsListDiv.appendChild(questionElement);
            });
        } catch (error) {
            console.error('문제 목록 로드 중 오류 발생:', error);
            availableQuestionsListDiv.innerHTML = '<p style="color: red;">문제 목록을 불러오는 데 실패했습니다.</p>';
        }
    };

    addSelectedQuestionsBtn.addEventListener('click', async () => {
        const selectedQuizId = selectQuizToAddQuestions.value;
        if (!selectedQuizId) {
            alert('문제를 추가할 퀴즈를 선택해주세요.');
            return;
        }

        const selectedQuestionIds = Array.from(document.querySelectorAll('.question-checkbox:checked'))
                                         .map(checkbox => parseInt(checkbox.value));

        if (selectedQuestionIds.length === 0) {
            alert('추가할 문제를 하나 이상 선택해주세요.');
            return;
        }

        try {
            const response = await fetch(`/api/quizzes/${selectedQuizId}/questions`, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ questionIds: selectedQuestionIds })
            });

            if (response.ok) {
                alert(`선택한 문제들이 퀴즈 ID ${selectedQuizId}에 성공적으로 추가되었습니다!`);
                loadAllQuestionsForQuizAddition(); // 문제 목록 새로고침 (어떤 퀴즈에 속하는지 표시 업데이트)
                loadQuizzes(); // 퀴즈 목록 새로고침 (문제 수 업데이트 등)
            } else {
                const errorText = await response.text();
                alert(`문제 추가 실패: ${errorText || response.statusText}`);
            }
        } catch (error) {
            console.error('퀴즈에 문제 추가 중 오류 발생:', error);
            alert('퀴즈에 문제 추가 중 오류가 발생했습니다.');
        }
    });


    // --- 초기 로딩 시 실행 ---
    loadQuizzes(); // 첫 화면은 퀴즈 목록
    toggleQuestionTypeFields(); // 문제 생성 폼 초기 상태
});