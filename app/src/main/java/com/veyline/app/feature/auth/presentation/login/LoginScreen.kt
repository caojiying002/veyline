package com.veyline.app.feature.auth.presentation.login

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.veyline.app.R
import com.veyline.app.ui.components.AppBackTopBar
import com.veyline.app.ui.components.AppButton
import com.veyline.app.ui.error.UiError
import com.veyline.app.ui.theme.SpacingExtraLarge
import com.veyline.app.ui.theme.SpacingLarge
import com.veyline.app.ui.theme.VeylineTheme
import com.veyline.app.ui.theme.appTextFieldColors
import com.veyline.app.ui.theme.withLineHeightFix

private const val VIEW_MODEL_KEY = "auth:login"

@Composable
fun LoginRoute(
    onNavigateBack: () -> Unit,
    onLoginSuccess: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginViewModel = hiltViewModel(
        key = VIEW_MODEL_KEY,
    ),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLoginSuccess by rememberUpdatedState(onLoginSuccess)
    val resources = LocalResources.current

    LaunchedEffect(viewModel, lifecycleOwner, context) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.effect.collect { effect ->
                when (effect) {
                    LoginEffect.LoginSucceeded -> currentOnLoginSuccess()

                    is LoginEffect.ShowGeneralError -> {
                        val message = when (val error = effect.error) {
                            UiError.Connection -> resources.getString(R.string.error_connection)
                            UiError.Technical -> resources.getString(R.string.error_technical)
                            is UiError.DisplayReady -> error.message
                        }
                        Toast.makeText(context, message, Toast.LENGTH_SHORT).show()     // 未来可能改用国内风格自定义的Toast类似物
                    }
                }
            }
        }
    }

    LoginScreen(
        uiState = uiState,
        onBackClick = onNavigateBack,
        onAction = viewModel::onAction,
        modifier = modifier,
    )
}

@Composable
fun LoginScreen(
    uiState: LoginUiState,
    onBackClick: () -> Unit,
    onAction: (LoginAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    var name by rememberSaveable { mutableStateOf("") }
    // 密码不做跨进程/配置变更的状态保存：rememberSaveable 最终会被系统写入 Bundle，
    // 理论上可能被日志、崩溃快照等途径间接捕获，密码不应该经过这条路径
    var password by remember { mutableStateOf("") }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // 提交逻辑单独抽出，供按钮点击和键盘“完成”操作共用同一入口
    val submitLogin = {
        if (!uiState.isSubmitting) {
            focusManager.clearFocus()
            keyboardController?.hide()
            onAction(LoginAction.Submit(name = name, password = password))
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VeylineTheme.colors.surface),   // 登录页全屏背景色surface，包括标题栏
    ) {
        AppBackTopBar(
            title = stringResource(R.string.action_back),
            onBackClick = onBackClick,
        )

        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
        ) {
            LoginWelcomeSection(
                modifier = Modifier.padding(
                    start = SpacingLarge,
                    top = 48.dp,
                    end = SpacingLarge,
                    bottom = 40.dp,
                ),
            )

            LoginNameField(
                value = name,
                onValueChange = { newValue ->
                    name = newValue
                    //  编辑用户名时清除用户名字段的旧错误
                    onAction(LoginAction.ClearNameError)
                },
                error = uiState.nameError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge),
            )

            Spacer(Modifier.height(SpacingLarge))

            LoginPasswordField(
                value = password,
                onValueChange = { newValue ->
                    password = newValue
                    // 编辑密码时清除密码字段的旧错误
                    onAction(LoginAction.ClearPasswordError)
                },
                error = uiState.passwordError,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge),
            )

            Spacer(Modifier.height(SpacingExtraLarge))

            AppButton(
                text = stringResource(R.string.action_login),   // 这里是否需要准备“登录中...”文案
                onClick = submitLogin,  // 未来键盘操作复用
                enabled = !uiState.isSubmitting,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = SpacingLarge),
            )
        }
    }
}

@Composable
private fun LoginNameField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.login_name_label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let {
            { Text(it) }
        },
        colors = appTextFieldColors(),
        modifier = modifier,
    )
}

@Composable
private fun LoginPasswordField(
    value: String,
    onValueChange: (String) -> Unit,
    error: String?,
    modifier: Modifier = Modifier,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(stringResource(R.string.login_password_label)) },
        singleLine = true,
        isError = error != null,
        supportingText = error?.let {
            { Text(it) }
        },
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Password,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(
            //onDone = submitLogin, // 这里访问不到
        ),
        colors = appTextFieldColors(),
        modifier = modifier,
    )
}

@Composable
private fun LoginWelcomeSection(
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = stringResource(R.string.login_welcome_title),
            style = TextStyle(
                fontSize = 30.sp,
                lineHeight = 40.sp,
                fontWeight = FontWeight.SemiBold,
            ).withLineHeightFix(),
            color = VeylineTheme.colors.textTitle,
        )

        Spacer(Modifier.height(10.dp))

        Text(
            text = stringResource(R.string.login_welcome_subtitle),
            style = TextStyle(
                fontSize = 15.sp,
                lineHeight = 24.sp,
            ).withLineHeightFix(),
            color = VeylineTheme.colors.textLight,
        )
    }
}
