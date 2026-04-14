# Huong Dan Them Va Fetch API Moi (drc-app)

Tai lieu nay huong dan cach them mot API moi theo dung architecture hien tai cua `drc-app`.

## 1. Kien truc tong quan
Flow chuan cua frontend:

`Fragment -> ViewModel -> UseCase -> Repository Interface -> Repository Impl -> ApiService (Retrofit)`

Neu can cache local thi mo rong them Room o layer repository.

Tham chieu implementation that:
- [drc-app/app/src/main/java/com/drc/aidbridge/ui/auth/fragment/GuestSosBottomSheet.java](app/src/main/java/com/drc/aidbridge/ui/auth/fragment/GuestSosBottomSheet.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/ui/auth/viewmodel/GuestSosViewModel.java](app/src/main/java/com/drc/aidbridge/ui/auth/viewmodel/GuestSosViewModel.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/domain/usecase/victim/UploadSosUseCase.java](app/src/main/java/com/drc/aidbridge/domain/usecase/victim/UploadSosUseCase.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/domain/repository/victim/VictimSosRepository.java](app/src/main/java/com/drc/aidbridge/domain/repository/victim/VictimSosRepository.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/repository/victim/VictimSosRepositoryImpl.java](app/src/main/java/com/drc/aidbridge/data/repository/victim/VictimSosRepositoryImpl.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/api/victim/SosApiService.java](app/src/main/java/com/drc/aidbridge/data/remote/api/victim/SosApiService.java)

## 2. Quy tac fetch API trong project
1. Tat ca API call tra ve `LiveData<NetworkResultWrapper<T>>` tu repository.
2. Repository phai parse `BaseResponse<T>` theo thu tu:
   - Check HTTP success.
   - Check `body != null`.
   - Check `body.isSuccess()`.
   - Lay `body.getData()` va `body.getMessage()` voi null safety.
3. ViewModel dung trigger-based `Transformations.switchMap(...)`.
4. Fragment chi collect input + observe state, khong goi Retrofit truc tiep.

Tham chieu:
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/NetworkResultWrapper.java](app/src/main/java/com/drc/aidbridge/data/remote/NetworkResultWrapper.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/repository/BaseRepository.java](app/src/main/java/com/drc/aidbridge/data/repository/BaseRepository.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/ui/base/BaseFragment.java](app/src/main/java/com/drc/aidbridge/ui/base/BaseFragment.java)

## 3. Checklist them API moi
### Buoc 1: Tao/Update API service
Vi tri: `app/src/main/java/com/drc/aidbridge/data/remote/api/...`

Vi du SOS:
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/api/victim/SosApiService.java](app/src/main/java/com/drc/aidbridge/data/remote/api/victim/SosApiService.java)

### Buoc 2: Tao request/response DTO
Vi tri:
- Request: `app/src/main/java/com/drc/aidbridge/data/remote/dto/request/...`
- Response: `app/src/main/java/com/drc/aidbridge/data/remote/dto/response/...`

Vi du:
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/dto/request/victim/CreateSosRequest.java](app/src/main/java/com/drc/aidbridge/data/remote/dto/request/victim/CreateSosRequest.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/dto/response/victim/SosRequestResponse.java](app/src/main/java/com/drc/aidbridge/data/remote/dto/response/victim/SosRequestResponse.java)

### Buoc 3: Tao method trong repository interface (domain)
Vi tri: `app/src/main/java/com/drc/aidbridge/domain/repository/...`

Vi du:
- [drc-app/app/src/main/java/com/drc/aidbridge/domain/repository/victim/VictimSosRepository.java](app/src/main/java/com/drc/aidbridge/domain/repository/victim/VictimSosRepository.java)

### Buoc 4: Implement trong repository impl (data)
Vi tri: `app/src/main/java/com/drc/aidbridge/data/repository/...`

Mau chung:
1. `MutableLiveData<NetworkResultWrapper<T>> result = new MutableLiveData<>();`
2. `result.postValue(NetworkResultWrapper.loading());`
3. `apiService.method(...).enqueue(...)`
4. Parse HTTP + BaseResponse + data/message
5. `result.postValue(NetworkResultWrapper.success(...))` hoac `.error(...)`

Vi du that:
- [drc-app/app/src/main/java/com/drc/aidbridge/data/repository/victim/VictimSosRepositoryImpl.java](app/src/main/java/com/drc/aidbridge/data/repository/victim/VictimSosRepositoryImpl.java)

### Buoc 5: Tao/Update UseCase
Vi tri: `app/src/main/java/com/drc/aidbridge/domain/usecase/...`

UseCase se:
- validate input (neu co)
- goi repository

Vi du:
- [drc-app/app/src/main/java/com/drc/aidbridge/domain/usecase/victim/UploadSosUseCase.java](app/src/main/java/com/drc/aidbridge/domain/usecase/victim/UploadSosUseCase.java)

### Buoc 6: Tao/Update ViewModel
Vi tri: `app/src/main/java/com/drc/aidbridge/ui/.../viewmodel/...`

Pattern bat buoc:
- Trigger: `MutableLiveData<Params>`
- Fetch: `Transformations.switchMap(trigger, params -> useCase.execute(...))`
- Expose: `LiveData<NetworkResultWrapper<T>>`

Vi du:
- [drc-app/app/src/main/java/com/drc/aidbridge/ui/auth/viewmodel/GuestSosViewModel.java](app/src/main/java/com/drc/aidbridge/ui/auth/viewmodel/GuestSosViewModel.java)

### Buoc 7: Update Fragment
Vi tri: `app/src/main/java/com/drc/aidbridge/ui/.../fragment/...`

Fragment phai:
- collect input
- goi viewModel.submit...
- observe result bang `resultObserver(...)` hoac xu ly `NetworkResultWrapper` dung lifecycle

Vi du:
- [drc-app/app/src/main/java/com/drc/aidbridge/ui/auth/fragment/GuestSosBottomSheet.java](app/src/main/java/com/drc/aidbridge/ui/auth/fragment/GuestSosBottomSheet.java)

### Buoc 8: Day DI
Cap nhat module neu them service/repository moi:
- [drc-app/app/src/main/java/com/drc/aidbridge/di/ApiModule.java](app/src/main/java/com/drc/aidbridge/di/ApiModule.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/di/RepositoryModule.java](app/src/main/java/com/drc/aidbridge/di/RepositoryModule.java)

Network stack tham chieu:
- [drc-app/app/src/main/java/com/drc/aidbridge/di/NetworkModule.java](app/src/main/java/com/drc/aidbridge/di/NetworkModule.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/interceptor/AuthInterceptor.java](app/src/main/java/com/drc/aidbridge/data/remote/interceptor/AuthInterceptor.java)
- [drc-app/app/src/main/java/com/drc/aidbridge/data/remote/interceptor/TokenRefreshInterceptor.java](app/src/main/java/com/drc/aidbridge/data/remote/interceptor/TokenRefreshInterceptor.java)

## 4. Vi du thuc te: fetch API sos-requests
### 4.1 API contract
Trong `SosApiService` da co:
- `@POST("sos-requests")`
- body: `CreateSosRequest`
- response: `BaseResponse<SosRequestResponse>`

### 4.2 Repository xu ly
`VictimSosRepositoryImpl`:
- Build request tu input UI + business text
- Map severity UI sang urgency level backend
- Goi API bat dong bo
- Parse response + fallback message
- Tra `NetworkResultWrapper<String>`

### 4.3 ViewModel trigger fetch
`GuestSosViewModel`:
- validate truoc
- set `submitTrigger`
- switchMap tu trigger sang `uploadSosUseCase.uploadSelfSos(...)`

### 4.4 Fragment render ket qua
`GuestSosBottomSheet`:
- lay location + du lieu form
- submit vao ViewModel
- render loading/success/error

## 5. Best practices khi them API moi
1. Khong goi Retrofit trong Fragment/ViewModel.
2. Luon tra `NetworkResultWrapper` o repository.
3. Luon co message fallback khi backend tra null.
4. Dung `switchMap` thay vi trigger truc tiep callback o Fragment.
5. Neu endpoint can auth, de interceptor tu them token; khong hardcode header trong Fragment.
6. Neu endpoint public (`/auth/*`), interceptor se bo qua auth header.
7. Dat ten file ro nghia va theo role package hien co.

## 6. Template nhanh (copy checklist)
- Tao endpoint trong ApiService
- Tao DTO request/response
- Tao method trong domain repository interface
- Implement method trong data repository impl
- Tao/Update UseCase
- Tao/Update ViewModel (switchMap trigger)
- Observe trong Fragment
- Cap nhat DI modules neu co class moi
- Test manual tren UI va check loading/error/success state

## 7. Tai lieu nen doc truoc khi code
- [docs/frontend_architecture.md](../docs/frontend_architecture.md)
- [docs/main/project_structure.md](../docs/main/project_structure.md)
- [docs/main/tech_stack.md](../docs/main/tech_stack.md)
- [.github/instructions/android.instructions.md](../.github/instructions/android.instructions.md)
