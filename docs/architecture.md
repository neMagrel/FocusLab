# Architecture

Focus Lab использует минимальный одноэкранный поток:

~~~text
MainActivity
  -> FocusLabTheme
      -> FocusRoute
          -> FocusViewModel
              -> FocusRepository
                  -> Preferences DataStore + FocusCodec
          -> FocusScreen
          -> CategoryEditor
~~~

- MainActivity отвечает только за setContent, theme и route.
- FocusRoute создаёт ViewModel, collect-ит FocusUiState, формирует callback contracts и host-ит transient CategoryEditor.
- FocusScreen - stateless student-facing Compose composition. Он не знает о DataStore, codec и wall-clock.
- FocusViewModel обрабатывает UI events, ticker, reconcile и transient completion feedback. Он не сериализует preferences.
- FocusRepository является границей durable state и атомарных mutations.
- DataStoreFocusRepository хранит categories, selection, progress и nullable ActiveFocusSession. FocusCodec изолирует формат хранения.

Level и remaining time являются derived values. Editor state и completion feedback являются transient. QA/checkpoints не добавляют product state.

В starter callback contracts уже переданы в FocusScreen, но student-owned Compose wiring намеренно отсутствует.
